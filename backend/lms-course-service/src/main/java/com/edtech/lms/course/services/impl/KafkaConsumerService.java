package com.edtech.lms.course.services.impl;

import com.edtech.lms.course.repositories.CompanyCourseAvailabilityRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final EnrollmentRepository enrollmentRepository;
    private final CompanyCourseAvailabilityRepository companyCourseAvailabilityRepository;

    @KafkaListener(topics = "company-deleted-topic", groupId = "course-service-group")
    @Transactional
    public void handleCompanyDeletedEvent(String companyId) {
        log.info("Received COMPANY_DELETED event for companyId: {}", companyId);
        
        try {
            enrollmentRepository.deleteByCompanyId(companyId);
            companyCourseAvailabilityRepository.deleteByCompanyId(companyId);
            log.info("Successfully deleted enrollments and course availabilities for companyId: {}", companyId);
        } catch (Exception e) {
            log.error("Failed to delete records for companyId: {}", companyId, e);
            throw e; // Rely on retry mechanisms or DLQ if necessary
        }
    }

    @KafkaListener(topics = "user-deleted-topic", groupId = "course-service-group")
    @Transactional
    public void handleUserDeletedEvent(String employeeId) {
        log.info("Received USER_DELETED event for employeeId: {}", employeeId);
        
        try {
            enrollmentRepository.deleteByEmployeeId(employeeId);
            log.info("Successfully deleted enrollments for employeeId: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to delete enrollments for employeeId: {}", employeeId, e);
            throw e;
        }
    }
}
