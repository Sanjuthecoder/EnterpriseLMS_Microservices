package com.edtech.lms.course.services.impl;

import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.services.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public Enrollment assignCourseToEmployee(String companyId, String employeeId, Long courseId) {
        log.info("Assigning course {} to employee {} in company {}", courseId, employeeId, companyId);
        
        // 1. Synchronous validation with Monolith (Identity Service later)
        // We will call the monolith to check if the user exists and belongs to the company
        try {
            String validationUrl = "http://enterprise-lms-backend/api/internal/users/validate?employeeId=" + employeeId + "&companyId=" + companyId;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(validationUrl, Boolean.class);
            if (response.getStatusCode() != HttpStatus.OK || Boolean.FALSE.equals(response.getBody())) {
                throw new IllegalArgumentException("Invalid employee or company mismatch");
            }
        } catch (Exception e) {
            log.error("Failed to validate employee {} with monolith", employeeId, e);
            throw new IllegalArgumentException("User validation failed");
        }

        // 2. Check if already enrolled
        Optional<Enrollment> existing = enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Employee is already enrolled in this course");
        }

        // 3. Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .companyId(companyId)
                .employeeId(employeeId)
                .courseId(courseId)
                .status(EnrollmentStatus.ASSIGNED)
                .progressPercentage(0)
                .build();
                
        return enrollmentRepository.save(enrollment);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "enrollments", key = "#employeeId + '-' + #companyId")
    public List<Enrollment> getEmployeeEnrollments(String companyId, String employeeId) {
        log.info("Fetching enrollments from database (Cache Miss) for employee: {}", employeeId);
        return enrollmentRepository.findByEmployeeIdAndCompanyId(employeeId, companyId);
    }

    @Override
    @Transactional
    public void updateProgress(String employeeId, Long courseId, Integer newProgress) {
        // Find enrollment without companyId since employeeId is unique across the system or we can pass companyId
        // In this simple version we assume employeeId + courseId is enough
        // For strict multi-tenancy, we should fetch by companyId as well.
        log.info("Updating progress for employee {} on course {} to {}%", employeeId, courseId, newProgress);
        // ... progress update logic
    }
}
