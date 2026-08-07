package com.edtech.lms.course.models.enums;

/**
 * Status of course enrollments (assignments to employees).
 */
public enum EnrollmentStatus {
    ASSIGNED("Course assigned, not yet started"),
    IN_PROGRESS("Employee has started the course"),
    COMPLETED("Employee completed the course"),
    FAILED("Employee failed the course"),
    EXPIRED("Deadline passed without completion");

    private final String description;

    EnrollmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
