package com.edtech.lms.telemetry.models.dtos.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TelemetryAggregationResponse {
    private String id;
    private Long orgId;
    private Long companyId;
    private Long employeeId;
    private Long courseId;
    private String metricType;
    private LocalDate period;
    private JsonNode patterns;
    private JsonNode suggestions;
    private JsonNode recommendations;
    private JsonNode metrics;
    private LocalDateTime computedAt;
}
