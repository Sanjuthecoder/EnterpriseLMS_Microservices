package com.edtech.lms.course.controllers;

import com.edtech.lms.course.models.dtos.EnrollmentResponse;
import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.mappers.CourseMapper;
import com.edtech.lms.course.services.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/assign")
    public ResponseEntity<EnrollmentResponse> assignCourse(
            @RequestParam String companyId,
            @RequestParam String employeeId,
            @RequestParam Long courseId) {
        Enrollment enrollment = enrollmentService.assignCourseToEmployee(companyId, employeeId, courseId);
        return new ResponseEntity<>(CourseMapper.toEnrollmentResponse(enrollment), HttpStatus.CREATED);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EnrollmentResponse>> getEmployeeEnrollments(
            @RequestParam String companyId,
            @PathVariable String employeeId) {
        List<EnrollmentResponse> enrollments = enrollmentService.getEmployeeEnrollments(companyId, employeeId)
                .stream()
                .map(CourseMapper::toEnrollmentResponse)
                .toList();
        return ResponseEntity.ok(enrollments);
    }
}
