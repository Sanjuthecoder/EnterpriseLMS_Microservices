package com.edtech.lms.course.controllers;

import com.edtech.lms.course.services.CompanyAdminCourseService;
import com.edtech.lms.course.models.dtos.CourseResponse;
import com.edtech.lms.course.mappers.CourseMapper;
import com.edtech.lms.course.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CompanyAdminCourseController — Company-scoped course and enrollment management.
 *
 * Routes: /api/company-admin/courses/**, /api/company-admin/enrollments/**
 * Multi-tenancy: companyId extracted from X-Company-Id header (injected by Gateway).
 */
@Slf4j
@RestController
@RequestMapping("/api/company-admin")
@RequiredArgsConstructor
public class CompanyAdminCourseController {

    private final CompanyAdminCourseService companyAdminCourseService;

    /**
     * GET /api/company-admin/courses
     * Returns courses enabled for the company by Super Admin.
     */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getAvailableCourses(
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        
        List<CourseResponse> courses = companyAdminCourseService.getAvailableCoursesForCompany(companyId)
                .stream()
                .map(CourseMapper::toCourseResponse)
                .toList();
                
        return ResponseEntity.ok(courses);
    }

    /**
     * GET /api/company-admin/enrollments
     * Returns all enrollment records with course titles.
     */
    @GetMapping("/enrollments")
    public ResponseEntity<?> getCompanyEnrollments(
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.getEnrollmentSummary(companyId));
    }

    /**
     * POST /api/company-admin/enrollments/batch
     * Batch enrolls employees into a course.
     * Body: { courseId, employeeIds: [], deadline: "2025-12-31T00:00:00" }
     */
    @PostMapping("/enrollments/batch")
    public ResponseEntity<?> batchEnrollEmployees(
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestBody Map<String, Object> body) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        
        Long courseId = Long.parseLong(body.get("courseId").toString());
        List<String> employeeIds = ((List<?>) body.get("employeeIds")).stream()
                .map(Object::toString)
                .toList();
        LocalDateTime deadline = body.containsKey("deadline") && body.get("deadline") != null
                ? LocalDateTime.parse(body.get("deadline").toString())
                : LocalDateTime.now().plusDays(90);

        Map<String, Object> report = companyAdminCourseService.batchEnroll(companyId, courseId, employeeIds, deadline);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/employees/{employeeId}/courses/{courseId}/quiz-telemetry")
    public ResponseEntity<?> getEmployeeQuizTelemetry(
            @PathVariable String employeeId, @PathVariable Long courseId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.getEmployeeQuizTelemetry(employeeId, courseId, companyId));
    }

    @GetMapping("/employees/{employeeId}/courses/{courseId}/uplift")
    public ResponseEntity<?> getEmployeeUpliftReport(
            @PathVariable String employeeId, @PathVariable Long courseId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.getEmployeeUpliftReport(employeeId, courseId, companyId));
    }

    @GetMapping("/employees/{employeeId}/courses/{courseId}/video-telemetry")
    public ResponseEntity<?> getEmployeeVideoTelemetry(
            @PathVariable String employeeId, @PathVariable Long courseId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.getEmployeeVideoTelemetry(employeeId, courseId, companyId));
    }

    // =========================================================================
    // CERTIFICATES
    // =========================================================================

    @GetMapping("/certificates/requests")
    public ResponseEntity<?> getCertificateRequests(@RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.getPendingCertificateRequests(companyId));
    }

    @PostMapping("/certificates/{enrollmentId}/approve")
    public ResponseEntity<?> approveCertificate(
            @PathVariable Long enrollmentId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.approveCertificate(companyId, enrollmentId));
    }

    @PostMapping("/certificates/{enrollmentId}/reject")
    public ResponseEntity<?> rejectCertificate(
            @PathVariable Long enrollmentId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Missing X-Company-Id header");
        }
        return ResponseEntity.ok(companyAdminCourseService.rejectCertificate(companyId, enrollmentId));
    }
}
