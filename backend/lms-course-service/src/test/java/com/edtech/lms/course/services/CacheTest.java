package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.services.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
public class CacheTest {

    @MockBean
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @Test
    public void testCacheableMethod() {
        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setEmployeeId("EMP999");
        when(enrollmentRepository.findByEmployeeIdAndCompanyId("EMP999", "COMP999"))
            .thenReturn(Collections.singletonList(mockEnrollment));

        // First call - should hit DB
        enrollmentService.getEmployeeEnrollments("COMP999", "EMP999");
        
        // Second call - should hit cache
        enrollmentService.getEmployeeEnrollments("COMP999", "EMP999");

        // Verify DB was only called once
        verify(enrollmentRepository, times(1)).findByEmployeeIdAndCompanyId("EMP999", "COMP999");
    }
}
