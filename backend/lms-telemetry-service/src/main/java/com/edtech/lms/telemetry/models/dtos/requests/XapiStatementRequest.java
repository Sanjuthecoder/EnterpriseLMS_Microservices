package com.edtech.lms.telemetry.models.dtos.requests;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class XapiStatementRequest {
    private Long companyId;
    private Long employeeId;
    private Long courseId;
    private JsonNode actor;
    private JsonNode verb;
    private JsonNode object;
    private JsonNode result;
    private JsonNode context;
    private LocalDateTime timestamp;
}
