package com.edtech.lms.telemetry.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * TelemetryAggregation Document - Pre-computed analytics for dashboards
 * 
 * Purpose:
 * 1. Pre-aggregate raw xAPI statements and video events
 * 2. Compute learning patterns (overwhelmed, confused, engaged, etc.)
 * 3. Generate AI recommendations
 * 4. Cache results for fast dashboard queries
 * 
 * This avoids expensive real-time aggregations by pre-computing metrics
 * every 5 minutes in a background job (Phase 2)
 * 
 * Example workflow:
 * 1. Employee takes quiz, xAPI statement stored → stored in xapi_statements
 * 2. Background job runs every 5 minutes
 * 3. Reads all xapi_statements for this employee today
 * 4. Computes patterns (hesitation_count > threshold = "confused")
 * 5. Runs SuggestionGeneratorService (reuses EdTech algorithm)
 * 6. Stores result in telemetry_aggregations
 * 7. Frontend queries this collection for instant dashboard load
 */
@Document(collection = "svc_telemetry_telemetry_aggregations")
@CompoundIndexes({
    @CompoundIndex(name = "idx_agg_org_company_employee", def = "{'org_id': 1, 'company_id': 1, 'employee_id': 1}"),
    @CompoundIndex(name = "idx_agg_period", def = "{'period': 1}"),
    @CompoundIndex(name = "idx_agg_course", def = "{'course_id': 1, 'period': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryAggregation {

    @Id
    private String id;

    @Indexed
    private Long orgId;

    @Indexed
    private Long companyId;

    @Indexed
    private Long employeeId;

    private Long courseId;

    /**
     * Type of metric: "learning_patterns", "time_to_competency", "skill_mastery", etc.
     */
    private String metricType;

    /**
     * Date this aggregation is for (e.g., "2026-06-16")
     * Enables daily/weekly/monthly rollups
     */
    @Indexed
    private LocalDate period;

    /**
     * Detected learning patterns
     * Example:
     * {
     *   "overwhelmed": false,
     *   "confused": true,
     *   "disengaged": false,
     *   "struggling": true,
     *   "engagement_level": 0.75,
     *   "cognitive_load": "medium"
     * }
     */
    private JsonNode patterns;

    /**
     * AI-generated suggestions based on patterns
     * Example:
     * ["Add annotations at 3m42s", "Break into micro-lessons", "Add summary slide"]
     */
    private JsonNode suggestions;

    /**
     * Recommended lessons/courses to take next
     * Example: ["lesson_301", "lesson_302", "course_45"]
     * (Uses 7-pattern algorithm from EdTech platform)
     */
    private JsonNode recommendations;

    /**
     * Detailed metrics
     * Example:
     * {
     *   "quiz_attempts": 3,
     *   "avg_quiz_score": 0.75,
     *   "hesitation_count": 5,
     *   "video_rewinds": 12,
     *   "avg_response_time": 45.2,
     *   "time_to_competency_days": 7
     * }
     */
    private JsonNode metrics;

    /**
     * When this aggregation was computed
     */
    @Builder.Default
    private LocalDateTime computedAt = LocalDateTime.now();
}
