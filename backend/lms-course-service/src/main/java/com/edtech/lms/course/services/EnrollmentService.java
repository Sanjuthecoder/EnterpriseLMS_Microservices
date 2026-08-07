package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Enrollment;
import java.util.List;

public interface EnrollmentService {
    Enrollment assignCourseToEmployee(String companyId, String employeeId, Long courseId);
    List<Enrollment> getEmployeeEnrollments(String companyId, String employeeId);
    void updateProgress(String employeeId, Long courseId, Integer newProgress);
}
