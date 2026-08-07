package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.ContentType;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.edtech.lms.course.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CreatorCourseService — All content creator operations.
 *
 * Responsibilities:
 * - Course CRUD (create, update, submit for approval)
 * - Lesson management (add/list lessons)
 * - Quiz question management (add/list questions)
 * - Media file upload (local filesystem)
 * - Dashboard stats (learner count, video lessons)
 *
 * Note: Only DRAFT courses can be edited. Submitted courses cannot be modified
 * until rejected and reverted to DRAFT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorCourseService {

    private final CourseRepository courseRepository;
    private final CourseStructureRepository courseStructureRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final EnrollmentRepository enrollmentRepository;

    private static final String UPLOAD_DIR = "uploads/";

    // =========================================================================
    // COURSE MANAGEMENT
    // =========================================================================

    @Transactional
    public Course createCourse(Course course) {
        log.info("Creating course: title={}, creatorId={}", course.getTitle(), course.getCreatorId());
        if (course.getOrgId() == null) {
            course.setOrgId("0"); // Platform-level course (independent creator)
        }
        course.setStatus(CourseStatus.DRAFT);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long courseId, Course updates) {
        Course course = getCourseOrThrow(courseId);
        if (course.getStatus() == CourseStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot edit a course that is pending approval.");
        }

        if (updates.getTitle() != null) course.setTitle(updates.getTitle());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        if (updates.getDifficultyLevel() != null) course.setDifficultyLevel(updates.getDifficultyLevel());
        if (updates.getDurationMinutes() != null) course.setDurationMinutes(updates.getDurationMinutes());

        return courseRepository.save(course);
    }

    @Transactional
    public Course submitForApproval(Long courseId) {
        log.info("Creator submitting course {} for approval", courseId);
        Course course = getCourseOrThrow(courseId);
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT courses can be submitted. Status: " + course.getStatus());
        }
        course.setStatus(CourseStatus.PENDING_APPROVAL);
        return courseRepository.save(course);
    }

    public List<Course> getCreatorCourses(String creatorId) {
        return courseRepository.findByCreatorId(creatorId);
    }

    // =========================================================================
    // LESSON MANAGEMENT
    // =========================================================================

    public List<CourseStructure> getLessons(Long courseId) {
        return courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId);
    }

    @Transactional
    public CourseStructure addLesson(Long courseId, CourseStructure lesson) {
        getCourseOrThrow(courseId); // Validate course exists
        lesson.setCourseId(courseId);
        return courseStructureRepository.save(lesson);
    }

    // =========================================================================
    // QUIZ MANAGEMENT
    // =========================================================================

    public List<QuizQuestion> getQuestions(Long courseId) {
        return quizQuestionRepository.findByCourseId(courseId);
    }

    /**
     * Adds a quiz question authored by a human creator.
     *
     * <p><b>Data hygiene invariant:</b> {@code isAiGenerated} is forcefully set to {@code false}
     * regardless of what the request body contains. This prevents any future ambiguity between
     * human-authored and AI-generated questions in the smart-routing query.
     *
     * @param courseId the course this question belongs to
     * @param question the question payload from the creator
     * @return the saved question entity
     */
    @Transactional
    public QuizQuestion addQuestion(Long courseId, QuizQuestion question) {
        getCourseOrThrow(courseId); // Validate course exists
        question.setCourseId(courseId);
        // Always override: creator questions are never AI-generated.
        question.setIsAiGenerated(false);
        return quizQuestionRepository.save(question);
    }

    // =========================================================================
    // MEDIA UPLOAD
    // =========================================================================

    /**
     * Uploads a media file to local filesystem and returns its accessible URL.
     * For production, swap this with S3/GCS object storage.
     */
    public String uploadMedia(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        log.info("Media uploaded: {}", filename);
        return "/uploads/" + filename;
    }

    // =========================================================================
    // STATS
    // =========================================================================

    public Map<String, Object> getCreatorStats(String creatorId) {
        long activeLearners = enrollmentRepository.countByCreatorIdAndStatuses(
                creatorId,
                List.of(com.edtech.lms.course.models.enums.EnrollmentStatus.ASSIGNED,
                        com.edtech.lms.course.models.enums.EnrollmentStatus.IN_PROGRESS));

        long videoLessons = courseStructureRepository
                .countLessonsByCreatorIdAndType(creatorId, ContentType.VIDEO);

        return Map.of("activeLearners", activeLearners, "videoLessons", videoLessons);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }
}
