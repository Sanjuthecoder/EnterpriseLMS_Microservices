package com.edtech.lms.course.models.enums;

/**
 * Difficulty levels for courses and questions.
 */
public enum DifficultyLevel {
    BEGINNER("Beginner level"),
    INTERMEDIATE("Intermediate level"),
    ADVANCED("Advanced level"),
    EXPERT("Expert level");

    private final String description;

    DifficultyLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
