package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.repositories.CompanyCourseAvailabilityRepository;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyAdminCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CompanyCourseAvailabilityRepository ccaRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseStructureRepository courseStructureRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CompanyAdminCourseService companyAdminCourseService;

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = new Enrollment();
        enrollment.setEnrollmentId(1L);
        enrollment.setCompanyId("TEST_COMPANY");
        enrollment.setCertificateStatus("REQUESTED");
    }

    @Test
    void approveCertificate_Success() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        companyAdminCourseService.approveCertificate("TEST_COMPANY", 1L);

        assertEquals("APPROVED", enrollment.getCertificateStatus());
        verify(enrollmentRepository, times(1)).save(enrollment);
    }

    @Test
    void approveCertificate_ThrowsSecurityException() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(SecurityException.class, () -> {
            companyAdminCourseService.approveCertificate("WRONG_COMPANY", 1L);
        });
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void approveCertificate_ThrowsIllegalStateException() {
        enrollment.setCertificateStatus("APPROVED");
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalStateException.class, () -> {
            companyAdminCourseService.approveCertificate("TEST_COMPANY", 1L);
        });
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void rejectCertificate_Success() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        companyAdminCourseService.rejectCertificate("TEST_COMPANY", 1L);

        assertEquals("REJECTED", enrollment.getCertificateStatus());
        verify(enrollmentRepository, times(1)).save(enrollment);
    }

    @Test
    void rejectCertificate_ThrowsSecurityException() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(SecurityException.class, () -> {
            companyAdminCourseService.rejectCertificate("WRONG_COMPANY", 1L);
        });
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void rejectCertificate_ThrowsIllegalStateException() {
        enrollment.setCertificateStatus("APPROVED");
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalStateException.class, () -> {
            companyAdminCourseService.rejectCertificate("TEST_COMPANY", 1L);
        });
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void getEmployeeQuizTelemetry_ReturnsPreQuizData() {
        enrollment.setLessonGatingMap(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        enrollment.setPreQuizScore(85.0);
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("EMP_1", 100L, "TEST_COMPANY"))
                .thenReturn(Optional.of(enrollment));

        org.springframework.http.ResponseEntity responseEntity = mock(org.springframework.http.ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(java.util.List.of("statement1", "statement2"));

        when(restTemplate.exchange(
                anyString(),
                eq(org.springframework.http.HttpMethod.GET),
                isNull(),
                any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        java.util.Map<String, Object> result = companyAdminCourseService.getEmployeeQuizTelemetry("EMP_1", 100L, "TEST_COMPANY");

        assertNotNull(result);
        assertEquals(85.0, result.get("preQuizScore"));
        assertNotNull(result.get("lessonGatingMap"));
        assertEquals(java.util.List.of("statement1", "statement2"), result.get("xapiStatements"));
    }
}
