package com.edtech.lms.telemetry.consumers;

import com.edtech.lms.telemetry.models.VideoTelemetry;
import com.edtech.lms.telemetry.models.XapiStatement;
import com.edtech.lms.telemetry.repositories.VideoTelemetryRepository;
import com.edtech.lms.telemetry.repositories.XapiStatementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryConsumer {

    private final VideoTelemetryRepository videoTelemetryRepository;
    private final XapiStatementRepository xapiStatementRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.video-telemetry:video-telemetry-topic}", groupId = "${app.kafka.group-id:telemetry-group}")
    public void consumeVideoTelemetry(String message) {
        try {
            processVideoTelemetryMessage(message);
        } catch (Exception ex) {
            log.error("Error processing video telemetry message: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to process video telemetry message", ex);
        }
    }

    private void processVideoTelemetryMessage(String message) throws Exception {
        JsonNode eventNode = objectMapper.readTree(message);
        Long employeeId = eventNode.get("employeeId").asLong();
        Long courseId = eventNode.get("courseId").asLong();
        Long lessonId = eventNode.get("lessonId").asLong();
        Long orgId = eventNode.get("orgId").asLong();
        Long companyId = eventNode.get("companyId").asLong();
        
        JsonNode requestNode = eventNode.get("request");
        String sessionId = requestNode.get("sessionId").asText();

        Optional<VideoTelemetry> existing = videoTelemetryRepository
                .findByEmployeeIdAndLessonIdAndSessionId(employeeId, lessonId, sessionId);

        List<JsonNode> eventNodes = new ArrayList<>();
        if (requestNode.has("events") && requestNode.get("events").isArray()) {
            for (JsonNode event : requestNode.get("events")) {
                eventNodes.add(event);
            }
        }

        VideoTelemetry telemetry = existing.isPresent()
                ? updateConsumerTelemetry(existing.get(), requestNode, eventNodes)
                : createConsumerTelemetry(employeeId, companyId, orgId, courseId, lessonId, sessionId, requestNode, eventNodes);

        videoTelemetryRepository.save(telemetry);
    }

    private VideoTelemetry updateConsumerTelemetry(VideoTelemetry telemetry, JsonNode requestNode, List<JsonNode> eventNodes) {
        telemetry.setEvents(eventNodes);
        if (requestNode.has("totalSeeks")) telemetry.setTotalSeeks(requestNode.get("totalSeeks").asInt());
        if (requestNode.has("highSpeedSeconds")) telemetry.setHighSpeedSeconds(requestNode.get("highSpeedSeconds").asInt());
        if (requestNode.has("completionPercentage")) telemetry.setCompletionPercentage(requestNode.get("completionPercentage").asInt());
        if (requestNode.has("completionStatus")) telemetry.setCompletionStatus(requestNode.get("completionStatus").asText());
        if (requestNode.has("duration")) telemetry.setVideoDuration(requestNode.get("duration").asInt());
        telemetry.setUpdatedAt(LocalDateTime.now());
        return telemetry;
    }

    private VideoTelemetry createConsumerTelemetry(Long employeeId, Long companyId, Long orgId, Long courseId, Long lessonId, String sessionId, JsonNode requestNode, List<JsonNode> eventNodes) {
        return VideoTelemetry.builder()
                .orgId(orgId)
                .companyId(companyId)
                .employeeId(employeeId)
                .courseId(courseId)
                .lessonId(lessonId)
                .sessionId(sessionId)
                .events(eventNodes)
                .totalSeeks(requestNode.has("totalSeeks") ? requestNode.get("totalSeeks").asInt() : 0)
                .highSpeedSeconds(requestNode.has("highSpeedSeconds") ? requestNode.get("highSpeedSeconds").asInt() : 0)
                .videoDuration(requestNode.has("duration") ? requestNode.get("duration").asInt() : 0)
                .completionPercentage(requestNode.has("completionPercentage") ? requestNode.get("completionPercentage").asInt() : 0)
                .completionStatus(requestNode.has("completionStatus") ? requestNode.get("completionStatus").asText() : "started")
                .build();
    }

    @KafkaListener(topics = "${app.kafka.topics.xapi-statements:xapi-statements-topic}", groupId = "${app.kafka.group-id:telemetry-group}")
    public void consumeXapiStatement(String message) {
        try {
            XapiStatement statement = objectMapper.readValue(message, XapiStatement.class);
            xapiStatementRepository.save(statement);
        } catch (Exception ex) {
            log.error("Error processing xapi statement message: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to process xapi statement message", ex);
        }
    }
}
