package com.edtech.lms.course.controllers;

import com.edtech.lms.course.services.EmployeeCourseService;
import com.edtech.lms.course.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * EmployeeController — REST API for all employee learning operations.
 *
 * Routes: /api/employees/**
 * Multi-tenancy: employeeId from X-User-Id, companyId from X-Company-Id (both injected by Gateway).
 *
 * Flow:
 *   GET  /dashboard                              → Enrolled courses + progress + gating
 *   GET  /courses/{courseId}/pre-quiz            → PRE_QUIZ questions (correct answers excluded)
 *   POST /courses/{courseId}/pre-quiz/submit     → 3-factor gating → lessonGatingMap
 *   GET  /courses/{courseId}/content             → Lessons with RECOMMENDED/OPTIONAL badges
 *   POST /courses/{courseId}/lessons/{id}/complete → Mark lesson done + recalculate progress
 *   GET  /courses/{courseId}/post-quiz           → POST_QUIZ questions
 *   POST /courses/{courseId}/post-quiz/submit    → Uplift calculation + completion
 */
@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeCourseService employeeCourseService;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (employeeId == null || companyId == null || employeeId.isBlank() || companyId.isBlank()) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.getEmployeeDashboard(employeeId, companyId));
    }

    @GetMapping("/test-exception")
    public ResponseEntity<?> testException() {
        employeeCourseService.getCourseContent("30001", "1", 1L);
        return ResponseEntity.ok("Success");
    }

    // =========================================================================
    // PRE-QUIZ
    // =========================================================================

    @GetMapping("/courses/{courseId}/pre-quiz")
    public ResponseEntity<?> getPreQuiz(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestHeader(value = "X-Subscription-Tier", defaultValue = "FREE") String subscriptionTier) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        
        List<Map<String, Object>> questions = employeeCourseService.getPreQuizQuestions(employeeId, companyId, courseId, subscriptionTier);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/courses/{courseId}/pre-quiz/submit")
    public ResponseEntity<?> submitPreQuiz(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestBody List<Map<String, Object>> answers) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        
        Map<String, Object> result = employeeCourseService.submitPreQuiz(employeeId, companyId, courseId, answers);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/courses/{courseId}/gating")
    public ResponseEntity<?> updateGatingMap(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestBody Map<String, Object> gatingRequest) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> lessonGating = (Map<String, String>) gatingRequest.get("lessonGatingMap");
        double preQuizScore = Double.parseDouble(gatingRequest.get("preQuizScore").toString());
        
        employeeCourseService.updateAiGating(employeeId, companyId, courseId, lessonGating, preQuizScore);
        return ResponseEntity.ok(Map.of("message", "Gating map updated via AI"));
    }

    // =========================================================================
    // COURSE CONTENT
    // =========================================================================

    @GetMapping("/courses/{courseId}/content")
    public ResponseEntity<?> getCourseContent(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.getCourseContent(employeeId, companyId, courseId));
    }

    // =========================================================================
    // LESSON COMPLETION
    // =========================================================================

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<?> markLessonComplete(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.markLessonComplete(employeeId, companyId, courseId, lessonId));
    }

    // =========================================================================
    // POST-QUIZ
    // =========================================================================

    @GetMapping("/courses/{courseId}/post-quiz")
    public ResponseEntity<?> getPostQuiz(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestHeader(value = "X-Subscription-Tier", defaultValue = "FREE") String subscriptionTier) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        List<Map<String, Object>> questions = employeeCourseService.getPostQuizQuestions(employeeId, companyId, courseId, subscriptionTier);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/courses/{courseId}/post-quiz/submit")
    public ResponseEntity<?> submitPostQuiz(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId,
            @RequestHeader(value = "X-Subscription-Tier", defaultValue = "FREE") String subscriptionTier,
            @RequestBody List<Map<String, Object>> answers) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.submitPostQuiz(employeeId, companyId, courseId, subscriptionTier, answers));
    }

    // =========================================================================
    // CERTIFICATES
    // =========================================================================

    @PostMapping("/courses/{courseId}/certificate/request")
    public ResponseEntity<?> requestCertificate(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.requestCertificate(employeeId, companyId, courseId));
    }

    @GetMapping("/courses/{courseId}/uplift")
    public ResponseEntity<?> getUpliftReport(
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String employeeId,
            @RequestHeader(value = "X-Company-Id", required = false) String companyId) {
        if (employeeId == null || companyId == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return ResponseEntity.ok(employeeCourseService.getUpliftReport(employeeId, companyId, courseId));
    }
}
