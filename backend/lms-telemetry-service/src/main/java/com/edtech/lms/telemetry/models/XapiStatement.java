package com.edtech.lms.telemetry.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * XapiStatement Document - Stores xAPI-compliant learning records
 * Used for quiz responses, video interactions, and other learning events
 * 
 * xAPI (Experience API) is a standard format for learning data:
 * - Actor: Who did the action (e.g., employee)
 * - Verb: What action they performed (answered, watched, completed)
 * - Object: What they performed the action on (quiz, video, lesson)
 * - Result: Outcome (score, success, response)
 * - Context: Additional context (org_id, company_id, etc.)
 * 
 * Stored in MongoDB for:
 * 1. Fast writes (no complex joins needed)
 * 2. Flexible schema (different learning events have different structures)
 * 3. Time-series indexing (efficient querying by date ranges)
 */
@Document(collection = "svc_telemetry_xapi_statements")
@CompoundIndexes({
    @CompoundIndex(name = "idx_xapi_org_company_employee", def = "{'org_id': 1, 'company_id': 1, 'employee_id': 1}"),
    @CompoundIndex(name = "idx_xapi_timestamp", def = "{'timestamp': 1}"),
    @CompoundIndex(name = "idx_xapi_course_employee", def = "{'course_id': 1, 'employee_id': 1, 'timestamp': -1}")
})
@org.springframework.data.annotation.TypeAlias("com.edtech.lms.models.documents.XapiStatement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XapiStatement {

    @Id
    private String id;

    @Indexed
    private Long orgId;

    @Indexed
    private Long companyId;

    @Indexed
    private Long employeeId;

    @Indexed
    private Long courseId;

    /**
     * Actor - Who performed the action
     * {
     *   "mbox": "employee@company.com",
     *   "name": "John Doe"
     * }
     */
    private JsonNode actor;

    /**
     * Verb - What action was performed
     * Example:
     * {
     *   "id": "http://adlnet.gov/expapi/verbs/answered",
     *   "display": "answered"
     * }
     * Common verbs: answered, completed, viewed, interacted, experienced
     */
    private JsonNode verb;

    /**
     * Object - What the action was performed on
     * {
     *   "id": "course123/quiz/q1",
     *   "definition": {
     *     "name": "Q1: React Fundamentals",
     *     "type": "question"
     *   }
     * }
     */
    private JsonNode object;

    /**
     * Result - Outcome of the action
     * {
     *   "success": true,
     *   "score": {
     *     "scaled": 0.8,
     *     "raw": 8,
     *     "min": 0,
     *     "max": 10
     *   },
     *   "response": "A",
     *   "duration": "PT5M42S"
     * }
     */
    private JsonNode result;

    /**
     * Context - Additional contextual information
     * {
     *   "extensions": {
     *     "hesitation_count": 2,
     *     "cognitive_load": "high",
     *     "trigger_reason": "pattern_detected"
     *   }
     * }
     */
    private JsonNode context;

    /**
     * When this learning event occurred
     */
    @Indexed
    private LocalDateTime timestamp;

    /**
     * When this record was stored in MongoDB
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
