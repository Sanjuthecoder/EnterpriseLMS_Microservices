package com.edtech.lms.course.models.dtos;

import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.models.enums.DifficultyLevel;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Course responses.
 * Prevents database entity leakage (like internal orgId constraints) to the client.
 */
@Data
@Builder
public class CourseResponse {
    private Long courseId;
    private String orgId;
    private String creatorId;
    private String title;
    private String description;
    private DifficultyLevel difficultyLevel;
    private CourseStatus status;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
