package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.CompanyCourseAvailability;
import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.repositories.CompanyCourseAvailabilityRepository;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

/**
 * SuperAdminCourseService — Platform-level course management for Super Admin.
 *
 * Responsibilities:
 * - Approve / Reject course submissions from Creators
 * - Enable / Disable courses for specific Companies
 * - Aggregate course performance metrics
 *
 * All operations are platform-wide (not scoped to a single company/org).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminCourseService {

    private final CourseRepository courseRepository;
    private final CompanyCourseAvailabilityRepository ccaRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseStructureRepository courseStructureRepository;
    private final VideoInsightService videoInsightService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.course-published:course-published-topic}")
    private String coursePublishedTopic;

    // =========================================================================
    // COURSE APPROVAL QUEUE
    // =========================================================================

    public Long getFirstVideoLessonId(Long courseId) {
        return courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId).stream()
                .filter(cs -> cs.getLessonType() == com.edtech.lms.course.models.enums.ContentType.VIDEO)
                .map(com.edtech.lms.course.models.entities.CourseStructure::getLessonId)
                .findFirst()
                .orElse(null);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> getPendingCourses() {
        return courseRepository.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.PENDING_APPROVAL)
                .toList();
    }

    @Transactional
    public Course approveCourse(Long courseId) {
        log.info("Approving course id={}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only PENDING_APPROVAL courses can be approved. Status: " + course.getStatus());
        }
        course.setStatus(CourseStatus.PUBLISHED);
        Course savedCourse = courseRepository.save(course);
        
        // Publish to Kafka for AI Service ingestion
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("courseId", savedCourse.getCourseId());
            payload.put("title", savedCourse.getTitle());
            payload.put("description", savedCourse.getDescription());
            payload.put("difficultyLevel", savedCourse.getDifficultyLevel());
            
            List<String> lessonSummaries = new ArrayList<>();
            List<com.edtech.lms.course.models.entities.CourseStructure> lessons = courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId);
            for (com.edtech.lms.course.models.entities.CourseStructure lesson : lessons) {
                lessonSummaries.add("Lesson ID " + lesson.getLessonId() + ": " + lesson.getTitle() 
                    + (lesson.getDescription() != null ? " - " + lesson.getDescription() : ""));
            }
            payload.put("lessons", lessonSummaries);
            
            kafkaTemplate.send(coursePublishedTopic, String.valueOf(courseId), objectMapper.writeValueAsString(payload));
            log.info("Published course {} to Kafka topic {}", courseId, coursePublishedTopic);
        } catch (Exception e) {
            log.error("Failed to publish course {} to Kafka: {}", courseId, e.getMessage());
        }
        
        return savedCourse;
    }

    @Transactional
    public Course rejectCourse(Long courseId) {
        log.info("Rejecting course id={}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        course.setStatus(CourseStatus.REJECTED);
        return courseRepository.save(course);
    }

    // =========================================================================
    // COMPANY COURSE DISTRIBUTION
    // =========================================================================

    @Transactional
    public CompanyCourseAvailability enableCourseForCompany(String companyId, Long courseId, String addedByUserId) {
        log.info("Enabling course {} for company {}", courseId, companyId);

        // Verify course is published
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED courses can be enabled for companies.");
        }

        // Check if already exists and update
        return ccaRepository.findByCompanyIdAndCourseId(companyId, courseId)
                .map(existing -> {
                    existing.setIsAvailable(true);
                    return ccaRepository.save(existing);
                })
                .orElseGet(() -> ccaRepository.save(CompanyCourseAvailability.builder()
                        .companyId(companyId)
                        .courseId(courseId)
                        .isAvailable(true)
                        .addedByUserId(addedByUserId)
                        .build()));
    }

    @Transactional
    public void disableCourseForCompany(String companyId, Long courseId) {
        log.info("Disabling course {} for company {}", courseId, companyId);
        ccaRepository.findByCompanyIdAndCourseId(companyId, courseId)
                .ifPresent(cca -> {
                    cca.setIsAvailable(false);
                    ccaRepository.save(cca);
                });
    }

    public List<CompanyCourseAvailability> getCourseAvailability(Long courseId) {
        return ccaRepository.findByCourseId(courseId);
    }

    // =========================================================================
    // INSIGHTS
    // =========================================================================

    @Transactional
    public Map<String, Object> triggerVideoInsightGeneration(Long courseId) {
        Long firstVideoLessonId = getFirstVideoLessonId(courseId);
        if (firstVideoLessonId == null) {
            throw new IllegalStateException("No video lessons found for this course.");
        }
        
        videoInsightService.publishInsightRequestForLesson(firstVideoLessonId, courseId);

        return Map.of(
                "message", "Video insight generation triggered for lesson.",
                "lessonId", firstVideoLessonId
        );
    }

    // =========================================================================
    // METRICS
    // =========================================================================

    public Map<String, Object> getCourseMetrics() {
        List<Course> all = courseRepository.findAll();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalCourses", all.size());
        metrics.put("publishedCourses", all.stream().filter(c -> c.getStatus() == CourseStatus.PUBLISHED).count());
        metrics.put("pendingApproval", all.stream().filter(c -> c.getStatus() == CourseStatus.PENDING_APPROVAL).count());
        metrics.put("draftCourses", all.stream().filter(c -> c.getStatus() == CourseStatus.DRAFT).count());
        return metrics;
    }

    public Map<String, Object> getCoursePerformance(Long courseId) {
        List<com.edtech.lms.course.models.entities.Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        
        if (enrollments.isEmpty()) {
            return Map.of(
                "completionRate", 0.0,
                "averageQuizScore", 0.0,
                "averageTimeToCompetencyDays", 0.0
            );
        }

        long completedCount = enrollments.stream()
                .filter(e -> e.getStatus() == com.edtech.lms.course.models.enums.EnrollmentStatus.COMPLETED)
                .count();

        double completionRate = (double) completedCount / enrollments.size() * 100;

        double avgQuizScore = enrollments.stream()
                .filter(e -> e.getPostQuizScore() != null)
                .mapToDouble(com.edtech.lms.course.models.entities.Enrollment::getPostQuizScore)
                .average()
                .orElse(0.0);

        double avgDaysToCompetency = enrollments.stream()
                .filter(e -> e.getStatus() == com.edtech.lms.course.models.enums.EnrollmentStatus.COMPLETED && e.getAssignedDate() != null && e.getCompletionDate() != null)
                .mapToDouble(e -> ChronoUnit.DAYS.between(e.getAssignedDate(), e.getCompletionDate()))
                .average()
                .orElse(0.0);

        return Map.of(
            "completionRate", completionRate,
            "averageQuizScore", avgQuizScore,
            "averageTimeToCompetencyDays", avgDaysToCompetency
        );
    }
}
