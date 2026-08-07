package com.edtech.lms.telemetry.models.dtos.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class XapiStatementResponse {
    private String id;
    private Long orgId;
    private Long companyId;
    private Long employeeId;
    private Long courseId;
    private JsonNode actor;
    private JsonNode verb;
    private JsonNode object;
    private JsonNode result;
    private JsonNode context;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
