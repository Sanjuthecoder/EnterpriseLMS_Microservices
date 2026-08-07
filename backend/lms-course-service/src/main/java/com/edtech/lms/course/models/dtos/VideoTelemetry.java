package com.edtech.lms.course.models.dtos;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

@Data
public class VideoTelemetry {
    private List<JsonNode> events;
    private Integer videoDuration;
    private Integer highSpeedSeconds;
    private Integer completionPercentage;
}
