package com.edtech.lms.course.controllers;

import com.edtech.lms.course.models.entities.CompanyCourseAvailability;
import com.edtech.lms.course.models.dtos.CourseResponse;
import com.edtech.lms.course.mappers.CourseMapper;
import com.edtech.lms.course.services.SuperAdminCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SuperAdminCourseController — Platform-level course management.
 *
 * Routes: /api/super-admin/courses/**, /api/super-admin/companies/**
 * Security: Gateway injects X-User-Role=SUPER_ADMIN. No companyId scoping.
 */
@Slf4j
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminCourseController {

    private final SuperAdminCourseService superAdminCourseService;

    // =========================================================================
    // COURSE APPROVAL QUEUE
    // =========================================================================

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(superAdminCourseService.getAllCourses().stream()
                .map(CourseMapper::toCourseResponse).toList());
    }

    @GetMapping("/courses/pending")
    public ResponseEntity<List<CourseResponse>> getPendingCourses() {
        return ResponseEntity.ok(superAdminCourseService.getPendingCourses().stream()
                .map(CourseMapper::toCourseResponse).toList());
    }

    @PatchMapping("/courses/{courseId}/approve")
    public ResponseEntity<CourseResponse> approveCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(CourseMapper.toCourseResponse(superAdminCourseService.approveCourse(courseId)));
    }

    @PatchMapping("/courses/{courseId}/reject")
    public ResponseEntity<CourseResponse> rejectCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(CourseMapper.toCourseResponse(superAdminCourseService.rejectCourse(courseId)));
    }

    // =========================================================================
    // COMPANY COURSE DISTRIBUTION
    // =========================================================================

    @PostMapping("/companies/{companyId}/courses/{courseId}")
    public ResponseEntity<CompanyCourseAvailability> enableCourseForCompany(
            @PathVariable String companyId,
            @PathVariable Long courseId,
            @RequestHeader(value = "X-User-Id", required = false) String addedByUserId) {
        String userId = addedByUserId != null ? addedByUserId : "super-admin";
        CompanyCourseAvailability cca = superAdminCourseService.enableCourseForCompany(companyId, courseId, userId);
        return ResponseEntity.ok(cca);
    }

    @DeleteMapping("/companies/{companyId}/courses/{courseId}")
    public ResponseEntity<?> disableCourseForCompany(
            @PathVariable String companyId,
            @PathVariable Long courseId) {
        superAdminCourseService.disableCourseForCompany(companyId, courseId);
        return ResponseEntity.ok(Map.of("message", "Course disabled for company successfully"));
    }

    @GetMapping("/courses/{courseId}/availability")
    public ResponseEntity<List<CompanyCourseAvailability>> getCourseAvailability(@PathVariable Long courseId) {
        return ResponseEntity.ok(superAdminCourseService.getCourseAvailability(courseId));
    }

    @GetMapping("/courses/metrics")
    public ResponseEntity<Map<String, Object>> getCourseMetrics() {
        return ResponseEntity.ok(superAdminCourseService.getCourseMetrics());
    }

    @GetMapping("/courses/{courseId}/performance")
    public ResponseEntity<Map<String, Object>> getCoursePerformance(@PathVariable Long courseId) {
        return ResponseEntity.ok(superAdminCourseService.getCoursePerformance(courseId));
    }

    @PostMapping("/courses/{courseId}/video-insights/generate")
    public ResponseEntity<Map<String, Object>> triggerVideoInsightGeneration(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(superAdminCourseService.triggerVideoInsightGeneration(courseId));
    }

}
