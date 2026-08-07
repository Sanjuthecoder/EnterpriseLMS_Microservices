package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.services.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetEmployeeEnrollments_Success() {
        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setEmployeeId("EMP123");

        when(enrollmentRepository.findByEmployeeIdAndCompanyId("EMP123", "COMP1"))
            .thenReturn(Collections.singletonList(mockEnrollment));

        List<Enrollment> res = enrollmentService.getEmployeeEnrollments("COMP1", "EMP123");
        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("EMP123", res.get(0).getEmployeeId());
    }
}
