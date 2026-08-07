package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.CompanyCourseAvailability;
import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.edtech.lms.course.repositories.CompanyCourseAvailabilityRepository;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CompanyAdminCourseService — Company-scoped course and enrollment management.
 *
 * Multi-tenancy: All methods require companyId to scope data access.
 * Responsibilities:
 * - List available courses for the company
 * - Batch enroll employees into courses
 * - View enrollment progress
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAdminCourseService {

    private final CourseRepository courseRepository;
    private final CompanyCourseAvailabilityRepository ccaRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseStructureRepository courseStructureRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.telemetry-service.url:http://lms-telemetry-service}")
    private String telemetryServiceUrl;

    /**
     * Returns all courses available to the company (enabled by Super Admin).
     */
    public List<Course> getAvailableCoursesForCompany(String companyId) {
        return courseRepository.findAvailableCoursesForCompany(companyId);
    }

    /**
     * Returns all enrollments for the company.
     */
    public List<Enrollment> getCompanyEnrollments(String companyId) {
        return enrollmentRepository.findByCompanyId(companyId);
    }

    /**
     * Batch enrolls a list of employees into a course.
     * Returns a report of successful and failed enrollments.
     */
    @Transactional
    public Map<String, Object> batchEnroll(String companyId, Long courseId, List<String> employeeIds, java.time.LocalDateTime deadline) {
        log.info("Batch enrolling {} employees into course {} for company {}", employeeIds.size(), courseId, companyId);

        // Validate course is available for this company
        Boolean available = ccaRepository.isCourseAvailableForCompany(companyId, courseId);
        if (Boolean.FALSE.equals(available)) {
            throw new IllegalArgumentException("Course " + courseId + " is not available for company " + companyId);
        }

        int success = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (String employeeId : employeeIds) {
            try {
                // Skip if already enrolled
                if (enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId).isPresent()) {
                    skipped++;
                    continue;
                }

                Enrollment enrollment = Enrollment.builder()
                        .companyId(companyId)
                        .employeeId(employeeId)
                        .courseId(courseId)
                        .status(EnrollmentStatus.ASSIGNED)
                        .deadline(deadline)
                        .build();

                enrollmentRepository.save(enrollment);
                success++;
            } catch (Exception e) {
                log.warn("Failed to enroll employee {}: {}", employeeId, e.getMessage());
                errors.add("Employee " + employeeId + ": " + e.getMessage());
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("successfullyEnrolled", success);
        report.put("skipped", skipped);
        report.put("failed", errors.size());
        report.put("errors", errors);
        return report;
    }

    /**
     * Builds an enhanced enrollment list with course title included.
     */
    public List<Map<String, Object>> getEnrollmentSummary(String companyId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCompanyId(companyId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Enrollment e : enrollments) {
            Map<String, Object> row = new HashMap<>();
            row.put("enrollmentId", e.getEnrollmentId());
            row.put("employeeId", e.getEmployeeId());
            row.put("courseId", e.getCourseId());
            row.put("status", e.getStatus().name());
            row.put("progressPercentage", e.getProgressPercentage());
            row.put("deadline", e.getDeadline());
            row.put("preQuizScore", e.getPreQuizScore());
            row.put("postQuizScore", e.getPostQuizScore());
            row.put("upliftPercent", e.getUpliftPercent());
            courseRepository.findById(e.getCourseId())
                    .ifPresent(c -> row.put("courseTitle", c.getTitle()));
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getEmployeeQuizTelemetry(String employeeId, Long courseId, String companyId) {
        String url = telemetryServiceUrl + "/api/v1/telemetry/xapi-statements?employeeId=" + employeeId + "&courseId=" + courseId + "&quizType=PRE_QUIZ";
        org.springframework.http.ResponseEntity<List<Object>> response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.GET, null, new org.springframework.core.ParameterizedTypeReference<List<Object>>() {});
        List<?> statements = response.getBody();
        Enrollment enrollment = enrollmentRepository
                .findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("xapiStatements", statements);
        result.put("lessonGatingMap", enrollment.getLessonGatingMap());
        result.put("preQuizScore", enrollment.getPreQuizScore());
        return result;
    }

    public Map<String, Object> getEmployeeUpliftReport(String employeeId, Long courseId, String companyId) {
        Enrollment enrollment = enrollmentRepository
                .findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("preQuizScore",   enrollment.getPreQuizScore());
        result.put("postQuizScore",  enrollment.getPostQuizScore());
        result.put("upliftPercent",  enrollment.getUpliftPercent());
        result.put("upliftReport",   enrollment.getUpliftReport());
        return result;
    }

    public List<Object> getEmployeeVideoTelemetry(String employeeId, Long courseId, String companyId) {
        String url = telemetryServiceUrl + "/api/v1/telemetry/video-sessions?employeeId=" + employeeId + "&courseId=" + courseId;
        org.springframework.http.ResponseEntity<List<Object>> response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.GET, null, new org.springframework.core.ParameterizedTypeReference<List<Object>>() {});
        return response.getBody();
    }

    public List<Map<String, Object>> getPendingCertificateRequests(String companyId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCompanyId(companyId);
        List<Map<String, Object>> pendingRequests = new ArrayList<>();
        for (Enrollment e : enrollments) {
            if ("REQUESTED".equals(e.getCertificateStatus())) {
                Map<String, Object> row = new HashMap<>();
                row.put("enrollmentId", e.getEnrollmentId());
                row.put("employeeId", e.getEmployeeId());
                row.put("courseId", e.getCourseId());
                row.put("requestedDate", e.getUpdatedAt()); // using updatedAt as proxy for request date
                courseRepository.findById(e.getCourseId()).ifPresent(c -> row.put("courseTitle", c.getTitle()));
                pendingRequests.add(row);
            }
        }
        return pendingRequests;
    }

    @Transactional
    public Map<String, Object> approveCertificate(String companyId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
                
        if (!enrollment.getCompanyId().equals(companyId)) {
            throw new SecurityException("Not authorized to approve for this company");
        }
        if (!"REQUESTED".equals(enrollment.getCertificateStatus())) {
            throw new IllegalStateException("Certificate is not in REQUESTED state");
        }
        
        enrollment.setCertificateStatus("APPROVED");
        enrollmentRepository.save(enrollment);
        return Map.of("message", "Certificate approved successfully");
    }

    @Transactional
    public Map<String, Object> rejectCertificate(String companyId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
                
        if (!enrollment.getCompanyId().equals(companyId)) {
            throw new SecurityException("Not authorized to reject for this company");
        }
        if (!"REQUESTED".equals(enrollment.getCertificateStatus())) {
            throw new IllegalStateException("Certificate is not in REQUESTED state");
        }
        
        enrollment.setCertificateStatus("REJECTED");
        enrollmentRepository.save(enrollment);
        return Map.of("message", "Certificate rejected successfully");
    }
}
