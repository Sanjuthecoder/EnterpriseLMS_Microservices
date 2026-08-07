package com.edtech.lms.course.models.enums;

/**
 * Status of courses.
 */
public enum CourseStatus {
    DRAFT("Work in progress"),
    PENDING_APPROVAL("Awaiting approval from company admin"),
    PUBLISHED("Available for enrollment"),
    ARCHIVED("No longer active"),
    REJECTED("Rejected by Super Admin");

    private final String description;

    CourseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
