package com.edtech.lms.course.models.enums;

/**
 * Content types in courses.
 */
public enum ContentType {
    VIDEO("Video content"),
    PDF("PDF document"),
    TEXT("Text content"),
    QUIZ("Assessment/Quiz"),
    INTERACTIVE("Interactive exercise");

    private final String description;

    ContentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
