package com.edtech.lms.ai.kafka;

import com.edtech.lms.ai.entity.CourseAiContext;
import com.edtech.lms.ai.repository.CourseAiContextRepository;
import com.edtech.lms.ai.service.AiQuizService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CourseLifecycleConsumer — Consumes course-published events and asynchronously generates
 * AI quiz questions, then publishes them back via the {@code ai-quiz-generated-topic}.
 *
 * <h2>Async Design</h2>
 * <p>The Kafka listener method immediately acknowledges the message and delegates quiz
 * generation to {@link AiQuizService#generateAndPublishCourseQuizzes(Long, String)} via
 * {@code @Async}. This prevents Kafka consumer timeout / rebalancing when Gemini takes
 * longer than {@code max.poll.interval.ms} (which can happen with rate-limit delays for
 * courses with many lessons).
 *
 * <h2>Idempotency</h2>
 * <p>The AI service tracks generated courseIds. If the same {@code COURSE_PUBLISHED} event
 * arrives twice, the {@code AiQuizService} idempotency guard skips re-generation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseLifecycleConsumer {

    private final ObjectMapper objectMapper;
    private final CourseAiContextRepository courseAiContextRepository;
    private final AiQuizService aiQuizService;

    /**
     * Consumes a {@code course-published-topic} message.
     *
     * <p>This method returns immediately after persisting the course AI context.
     * The actual Gemini API call is dispatched asynchronously via {@code @Async}
     * to avoid blocking the consumer thread past {@code max.poll.interval.ms}.
     *
     * @param message raw JSON string with course metadata and lesson list
     */
    @KafkaListener(topics = "course-published-topic", groupId = "ai-service-group")
    public void consumeCoursePublished(final String message) {
        try {
            final JsonNode payload = objectMapper.readTree(message);
            final Long courseId = payload.get("courseId").asLong();
            final String title = payload.has("title") ? payload.get("title").asText() : "";
            final String description = payload.has("description") ? payload.get("description").asText() : "";
            final String difficultyLevel = payload.has("difficultyLevel")
                    ? payload.get("difficultyLevel").asText() : "BEGINNER";

            // Build structured lesson summaries preserving lessonId for Gemini prompt context
            final List<String> lessonSummaries = parseLessonSummaries(payload);

            final String fullContext = buildFullContext(title, description, difficultyLevel, lessonSummaries);

            // Upsert the course AI context — preserve quiz questions if they already exist
            upsertCourseContext(courseId, title, description, difficultyLevel, lessonSummaries, fullContext);

            log.info("Course context upserted for courseId={}. Dispatching async quiz generation.", courseId);

            // Dispatch quiz generation asynchronously — Kafka message is already acknowledged.
            // This prevents the Kafka rebalancing death loop caused by blocking 15+ seconds.
            aiQuizService.generateAndPublishCourseQuizzes(courseId);

        } catch (Exception e) {
            log.error("Failed to process COURSE_PUBLISHED event: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Parses the lessons array from the Kafka payload into a list of context strings.
     * Each entry includes the lesson ID so Gemini can link questions back to lessons.
     *
     * @param payload the full Kafka message JSON
     * @return ordered list of lesson context strings
     */
    private List<String> parseLessonSummaries(final JsonNode payload) {
        final List<String> summaries = new ArrayList<>();
        if (payload.has("lessons") && payload.get("lessons").isArray()) {
            for (final JsonNode lessonNode : payload.get("lessons")) {
                final Long lessonId = lessonNode.path("lessonId").asLong(0);
                final String lessonTitle = lessonNode.path("title").asText("");
                final String lessonDesc = lessonNode.path("description").asText("");
                summaries.add("Lesson ID " + lessonId + ": " + lessonTitle
                        + (lessonDesc.isBlank() ? "" : " - " + lessonDesc));
            }
        }
        if (summaries.isEmpty()) {
            summaries.add("No lessons available.");
        }
        return summaries;
    }

    /**
     * Builds the full text context string injected into Gemini prompts.
     *
     * @param title          course title
     * @param description    course description
     * @param difficultyLevel difficulty level string
     * @param lessonSummaries list of lesson context strings
     * @return concatenated context string
     */
    private String buildFullContext(
            final String title, final String description,
            final String difficultyLevel, final List<String> lessonSummaries) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Course Title: ").append(title).append("\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Difficulty: ").append(difficultyLevel).append("\n");
        sb.append("Lessons:\n");
        lessonSummaries.forEach(lesson -> sb.append("  - ").append(lesson).append("\n"));
        return sb.toString();
    }

    /**
     * Upserts the {@link CourseAiContext} document in MongoDB.
     * Existing quiz questions are intentionally preserved during updates.
     */
    private void upsertCourseContext(
            final Long courseId, final String title, final String description,
            final String difficultyLevel, final List<String> lessonSummaries,
            final String fullContext) {

        courseAiContextRepository.findByCourseId(courseId).ifPresentOrElse(
            existing -> {
                existing.setFullContext(fullContext);
                existing.setLessonSummaries(lessonSummaries);
                existing.setCourseTitle(title);
                existing.setCourseDescription(description);
                existing.setDifficultyLevel(difficultyLevel);
                existing.setUpdatedAt(LocalDateTime.now());
                // NOTE: preQuizQuestions / postQuizQuestions are NOT touched here.
                // They are managed exclusively by AiQuizService.generateAndPublishCourseQuizzes()
                courseAiContextRepository.save(existing);
                log.info("Updated AI context for courseId={}", courseId);
            },
            () -> {
                final CourseAiContext context = CourseAiContext.builder()
                        .courseId(courseId)
                        .courseTitle(title)
                        .courseDescription(description)
                        .difficultyLevel(difficultyLevel)
                        .lessonSummaries(lessonSummaries)
                        .fullContext(fullContext)
                        .build();
                courseAiContextRepository.save(context);
                log.info("Created new AI context for courseId={}", courseId);
            }
        );
    }
}
