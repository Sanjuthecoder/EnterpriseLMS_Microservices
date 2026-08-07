package com.edtech.lms.ai.service;

import com.edtech.lms.ai.constant.AiConstants;
import com.edtech.lms.ai.client.GeminiClient;
import com.edtech.lms.ai.dto.response.AiInsightResponse;
import com.edtech.lms.ai.entity.AiInsightReport;
import com.edtech.lms.ai.repository.AiInsightReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AiInsightService — Processes raw telemetry metrics from Kafka and generates detailed AI suggestions.
 *
 * Data flow:
 * 1. Receives raw computed metrics (rewinds, skips, pauses, etc.) via Kafka from course-service.
 * 2. Passes the raw variables to Gemini with a detailed instructional design prompt.
 * 3. Gemini evaluates thresholds dynamically and generates a professional report.
 * 4. Stores generated AiInsightReport documents in svc_ai_insights collection.
 * 5. Serves reports to Super Admins and Creators via AiController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final AiInsightReportRepository insightReportRepository;
    private final GeminiClient geminiClient;

    @Value("classpath:templates/prompts.json")
    private org.springframework.core.io.Resource promptsResource;

    private String systemInsightPrompt;

    @jakarta.annotation.PostConstruct
    public void initPrompts() throws java.io.IOException {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(promptsResource.getInputStream());
        this.systemInsightPrompt = root.path("system-insight").asText();
    }

    // =========================================================================
    // KAFKA EVENT PROCESSING
    // =========================================================================

    /**
     * Called by VideoInsightKafkaConsumer when a metrics payload is received.
     */
    public void processInsightRequest(Map<String, Object> payload) {
        Long lessonId = Long.valueOf(payload.get("lessonId").toString());
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        int sessionCount = Integer.parseInt(payload.get("sessionCount").toString());

        if (sessionCount == 0) {
            log.info("Skipping insight generation for lessonId={} (0 sessions)", lessonId);
            return;
        }

        String userPrompt = buildDetailedPrompt(payload);
        
        try {
            // Generate detailed insight report using Gemini
            String aiGeneratedReport = geminiClient.generateText(systemInsightPrompt, userPrompt);
            
            // Clean up Markdown blocks if Gemini returns them
            if (aiGeneratedReport.startsWith("```json")) {
                aiGeneratedReport = aiGeneratedReport.substring(7, aiGeneratedReport.length() - 3).trim();
            } else if (aiGeneratedReport.startsWith("```")) {
                aiGeneratedReport = aiGeneratedReport.substring(3, aiGeneratedReport.length() - 3).trim();
            }

            AiInsightReport report = AiInsightReport.builder()
                    .lessonId(lessonId)
                    .courseId(courseId)
                    .insightScope("PLATFORM") // Global insights for Creator/Super Admin
                    .companyId(null)
                    .insightSummary("AI Detailed Analysis")
                    .creatorSuggestion(aiGeneratedReport)
                    .sessionsAnalyzed(sessionCount)
                    .build();

            // Overwrite existing PLATFORM report for this lesson if it exists
            Optional<AiInsightReport> existing = insightReportRepository.findByLessonIdAndInsightScope(lessonId, "PLATFORM").stream().findFirst();
            if (existing.isPresent()) {
                report.setId(existing.get().getId()); // Update existing
            }

            insightReportRepository.save(report);
            log.info("Successfully generated and saved AI insight for lessonId={}", lessonId);

        } catch (Exception e) {
            log.error("Failed to generate AI insight for lessonId={}: {}", lessonId, e.getMessage(), e);
        }
    }

    private String buildDetailedPrompt(Map<String, Object> payload) {
        return String.format(
            "Here is the raw video telemetry data for a lesson (based on %s sessions):\n" +
            "- Average Rewinds: %s per session\n" +
            "- Average Skips: %s per session\n" +
            "- Average Pauses: %s per session\n" +
            "- High-Speed Watch: %s%% of the video duration\n" +
            "- Completion Rate: %s%% of users finished the video\n" +
            "- Confusion Hotspot (highest rewinds): at %s seconds\n" +
            "- Boredom Hotspot (highest skips): at %s seconds\n\n" +
            "Please analyze this data and generate a detailed instructional design report.",
            payload.get("sessionCount"),
            payload.get("avgRewinds"),
            payload.get("avgSkips"),
            payload.get("avgPauses"),
            payload.get("avgSpeed"),
            payload.get("completionRate"),
            payload.get("topRewindSeconds") != null ? payload.get("topRewindSeconds") : "None",
            payload.get("topSkipSeconds") != null ? payload.get("topSkipSeconds") : "None"
        );
    }

    // =========================================================================
    // API FETCH METHODS
    // =========================================================================

    public List<AiInsightResponse> getCompanyInsights(final Long courseId, final String companyId) {
        return insightReportRepository
                .findByCourseIdAndCompanyIdAndInsightScope(courseId, companyId, "COMPANY")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AiInsightResponse> getPlatformInsights(final Long courseId) {
        return insightReportRepository
                .findByCourseIdAndInsightScope(courseId, "PLATFORM")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AiInsightResponse mapToResponse(final AiInsightReport report) {
        return AiInsightResponse.builder()
                .reportId(report.getId())
                .courseId(report.getCourseId())
                .lessonId(report.getLessonId())
                .insightScope(report.getInsightScope())
                .insightSummary(report.getInsightSummary())
                .creatorSuggestion(report.getCreatorSuggestion())
                .sessionsAnalyzed(report.getSessionsAnalyzed())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}
