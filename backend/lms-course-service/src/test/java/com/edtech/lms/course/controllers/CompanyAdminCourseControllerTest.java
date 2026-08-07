package com.edtech.lms.course.controllers;

import com.edtech.lms.course.services.CompanyAdminCourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyAdminCourseControllerTest {

    @Mock
    private CompanyAdminCourseService companyAdminCourseService;

    @InjectMocks
    private CompanyAdminCourseController controller;

    @Test
    void testBatchEnroll_WithIntegerIds_ShouldSucceed() {
        // Prepare a request body where Jackson deserializes JSON numbers as Integers
        Map<String, Object> body = Map.of(
                "courseId", 101,
                "employeeIds", List.of(1, 2, 3)
        );

        Map<String, Object> mockReport = Map.of("successfullyEnrolled", 3);
        when(companyAdminCourseService.batchEnroll(eq("comp1"), eq(101L), eq(List.of("1", "2", "3")), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        ResponseEntity<?> response = controller.batchEnrollEmployees("comp1", body);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockReport, response.getBody());
        
        verify(companyAdminCourseService).batchEnroll(eq("comp1"), eq(101L), eq(List.of("1", "2", "3")), any(LocalDateTime.class));
    }
}
