package com.edtech.lms.telemetry.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * VideoTelemetry Document - Stores video interaction events
 * Tracks: play, pause, rewind, seek, speed changes, etc.
 * 
 * Stored in MongoDB for:
 * 1. Event stream storage (array of events)
 * 2. Real-time processing (no joins needed)
 * 3. Analytical queries (aggregations on event types)
 */
@Document(collection = "svc_telemetry_video_telemetry")
@CompoundIndexes({
    @CompoundIndex(name = "idx_video_org_company_employee", def = "{'org_id': 1, 'company_id': 1, 'employee_id': 1}"),
    @CompoundIndex(name = "idx_video_course_lesson", def = "{'course_id': 1, 'lesson_id': 1}"),
    @CompoundIndex(name = "idx_video_timestamp", def = "{'createdAt': 1}")
})
@org.springframework.data.annotation.TypeAlias("com.edtech.lms.models.documents.VideoTelemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTelemetry {

    @Id
    private String id;

    @Indexed
    private Long orgId;

    @Indexed
    private Long companyId;

    @Indexed
    private Long employeeId;

    private Long courseId;

    private Long lessonId;

    /**
     * Unique per play session. Re-playing the same lesson creates a new sessionId.
     * This enables distinguishing first-watch vs re-watch behaviour in aggregates.
     */
    @Indexed
    private String sessionId;

    /**
     * Array of video interaction events
     * Example events:
     * {
     *   "type": "play",
     *   "videoTime": 0,
     *   "timestamp": "2026-06-16T10:00:00Z"
     * }
     * {
     *   "type": "pause",
     *   "videoTime": 125.5,
     *   "pauseDuration": 30,
     *   "timestamp": "2026-06-16T10:02:05Z"
     * }
     * {
     *   "type": "rewind",
     *   "fromTime": 200,
     *   "toTime": 150,
     *   "timestamp": "2026-06-16T10:03:00Z"
     * }
     * {
     *   "type": "speed_change",
     *   "speed": 1.5,
     *   "timestamp": "2026-06-16T10:04:00Z"
     * }
     */
    private List<JsonNode> events;

    /**
     * Total number of times user seeked in video
     */
    private Integer totalSeeks;

    /**
     * Total seconds watched at speed > 1x
     */
    private Integer highSpeedSeconds;

    /**
     * Total seconds of actual watch time (excluding pauses)
     */
    private Integer totalWatchTime;

    /**
     * Total video duration in seconds
     */
    private Integer videoDuration;

    /**
     * Completion status: not_started, in_progress, completed
     */
    private String completionStatus;

    /**
     * Percentage of video watched (0-100)
     */
    private Integer completionPercentage;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Explicit getters to guarantee compilation of VideoInsightService
    public List<JsonNode> getEvents() {
        return this.events;
    }

    public Integer getVideoDuration() {
        return this.videoDuration;
    }

    public Integer getHighSpeedSeconds() {
        return this.highSpeedSeconds;
    }

    public Integer getCompletionPercentage() {
        return this.completionPercentage;
    }
}
