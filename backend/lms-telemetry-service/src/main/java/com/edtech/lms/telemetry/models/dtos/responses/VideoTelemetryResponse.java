package com.edtech.lms.telemetry.models.dtos.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VideoTelemetryResponse {
    private String id;
    private Long orgId;
    private Long companyId;
    private Long employeeId;
    private Long courseId;
    private Long lessonId;
    private String sessionId;
    private List<JsonNode> events;
    private Integer totalSeeks;
    private Integer highSpeedSeconds;
    private Integer totalWatchTime;
    private Integer videoDuration;
    private String completionStatus;
    private Integer completionPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
