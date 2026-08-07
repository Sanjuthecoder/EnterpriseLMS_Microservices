package com.edtech.lms.course.services;

import com.edtech.lms.course.models.dtos.VideoTelemetry;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.enums.ContentType;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * VideoInsightService — Metrics aggregation engine.
 *
 * Processes ALL VideoTelemetry documents for a given lessonId across all companies/employees.
 * Computes raw averages (rewinds, skips, etc.) and publishes a Kafka event to lms-ai-service.
 * AI service determines thresholds and generates natural language suggestions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoInsightService {

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CourseStructureRepository courseStructureRepository;

    @Value("${app.telemetry-service.url:http://lms-telemetry-service}")
    private String telemetryServiceUrl;

    @Value("${app.kafka.topics.video-insight-request:video-insight-request-topic}")
    private String videoInsightRequestTopic;

    /**
     * Scheduled job to run daily and push telemetry metrics for all video lessons to AI service.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void generateDailyInsights() {
        log.info("Starting daily video insight metrics generation...");
        List<CourseStructure> videoLessons = courseStructureRepository.findAll().stream()
                .filter(cs -> cs.getLessonType() == ContentType.VIDEO)
                .toList();

        for (CourseStructure lesson : videoLessons) {
            publishInsightRequestForLesson(lesson.getLessonId(), lesson.getCourseId());
        }
        log.info("Finished publishing daily video insight requests.");
    }

    /**
     * Publishes metrics for a single lesson to Kafka. Can be triggered manually by Super Admin.
     */
    public void publishInsightRequestForLesson(Long lessonId, Long courseId) {
        String url = telemetryServiceUrl + "/api/telemetry/insights/lessons/" + lessonId + "/video-sessions";
        ResponseEntity<List<VideoTelemetry>> telemetryResponse;
        try {
            telemetryResponse = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<VideoTelemetry>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch telemetry for lessonId={}: {}", lessonId, e.getMessage());
            return;
        }

        List<VideoTelemetry> sessions = telemetryResponse.getBody() != null ? telemetryResponse.getBody() : new ArrayList<>();
        if (sessions.isEmpty()) {
            return; // No data, skip
        }

        // === Compute aggregate metrics ===
        List<Double> rewindCounts = new ArrayList<>();
        List<Double> skipCounts   = new ArrayList<>();
        List<Double> pauseCounts  = new ArrayList<>();
        List<Double> speedPcts    = new ArrayList<>();
        List<Integer> completions = new ArrayList<>();
        List<Double> rawRewindTimes = new ArrayList<>();
        List<Double> rawSkipTimes   = new ArrayList<>();

        for (VideoTelemetry s : sessions) {
            double rewinds = 0, skips = 0, pauses = 0;

            if (s.getEvents() != null) {
                for (JsonNode e : s.getEvents()) {
                    String type = e.path("type").asText("");
                    double videoTime = e.path("videoTime").asDouble(0);
                    switch (type) {
                        case "rewind"       -> { rewinds++; rawRewindTimes.add(videoTime); }
                        case "skip_forward" -> { skips++;   rawSkipTimes.add(videoTime); }
                        case "pause"        -> pauses++;
                    }
                }
            }

            rewindCounts.add(rewinds);
            skipCounts.add(skips);
            pauseCounts.add(pauses);

            if (s.getVideoDuration() != null && s.getVideoDuration() > 0 && s.getHighSpeedSeconds() != null) {
                speedPcts.add((s.getHighSpeedSeconds() * 100.0) / s.getVideoDuration());
            } else {
                speedPcts.add(0.0);
            }

            if (s.getCompletionPercentage() != null) {
                completions.add(s.getCompletionPercentage());
            }
        }

        double avgRewinds   = average(rewindCounts);
        double avgSkips     = average(skipCounts);
        double avgPauses    = average(pauseCounts);
        double avgSpeed     = average(speedPcts);
        double completionRate = completions.isEmpty() ? 0.0 :
                completions.stream().filter(c -> c >= 80).count() * 100.0 / completions.size();

        Map.Entry<Integer, Integer> topRewind = getTopHotspot(rawRewindTimes);
        Map.Entry<Integer, Integer> topSkip   = getTopHotspot(rawSkipTimes);

        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", lessonId);
        payload.put("courseId", courseId);
        payload.put("avgRewinds", round(avgRewinds));
        payload.put("avgSkips", round(avgSkips));
        payload.put("avgPauses", round(avgPauses));
        payload.put("avgSpeed", round(avgSpeed));
        payload.put("completionRate", round(completionRate));
        payload.put("topRewindSeconds", topRewind != null ? topRewind.getKey() : null);
        payload.put("topSkipSeconds", topSkip != null ? topSkip.getKey() : null);
        payload.put("sessionCount", sessions.size());

        try {
            kafkaTemplate.send(videoInsightRequestTopic, String.valueOf(lessonId), objectMapper.writeValueAsString(payload));
            log.info("Published insight request for lessonId={}", lessonId);
        } catch (Exception e) {
            log.error("Failed to publish insight request for lessonId={}: {}", lessonId, e.getMessage());
        }
    }

    private Map.Entry<Integer, Integer> getTopHotspot(List<Double> times) {
        if (times == null || times.isEmpty()) return null;
        Map<Integer, Integer> buckets = new HashMap<>();
        for (Double t : times) {
            int bucket = (int) (Math.floor(t / 10) * 10);
            buckets.merge(bucket, 1, Integer::sum);
        }
        return buckets.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private double average(List<Double> list) {
        if (list == null || list.isEmpty()) return 0.0;
        return list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
