package com.edtech.lms.course.models.dtos;

import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Enrollment responses.
 */
@Data
@Builder
public class EnrollmentResponse {
    private Long enrollmentId;
    private String companyId;
    private String employeeId;
    private Long courseId;
    private EnrollmentStatus status;
    private LocalDateTime assignedDate;
    private LocalDateTime deadline;
    private LocalDateTime completionDate;
    private Integer progressPercentage;
    private Double score;
    private JsonNode recommendations;
    private JsonNode lessonGatingMap;
    private JsonNode completedLessons;
    private Double preQuizScore;
    private Double postQuizScore;
    private Double upliftPercent;
    private JsonNode upliftReport;
    private String certificateStatus;
}
