package com.edtech.lms.course.services.impl;

import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.DifficultyLevel;
import com.edtech.lms.course.models.enums.QuizType;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * AiQuizKafkaConsumer — Listens to the {@code ai-quiz-generated-topic} and persists
 * AI-generated quiz questions into the MySQL course database.
 *
 * <h2>Why questions are stored here, not in the AI service</h2>
 * <p>The AI service is a "Content Engine" — it generates questions but does not own them.
 * Persisting questions in the course service allows the existing 3-factor gating engine,
 * xAPI telemetry, and submission logic to work without any changes.
 *
 * <h2>Idempotency</h2>
 * <p>Before inserting the incoming payload, this consumer <strong>deletes all existing
 * AI-generated questions for that courseId</strong>. This makes re-delivery safe:
 * the result is always the latest AI-generated set, with zero duplicates.
 *
 * <h2>Schema contract with AI service</h2>
 * <p>Expected JSON payload shape from {@code AI_QUIZ_GENERATED} topic:
 * <pre>{@code
 * {
 *   "courseId": 42,
 *   "questions": [
 *     {
 *       "lessonId": 100,
 *       "concept": "Dependency Injection",
 *       "questionText": "What does DI solve?",
 *       "options": ["A", "B", "C", "D"],
 *       "correctAnswer": "B",
 *       "quizType": "PRE_QUIZ",
 *       "difficultyRating": "INTERMEDIATE"
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuizKafkaConsumer {

    /** Kafka topic name where the AI service publishes generated quiz payloads. */
    public static final String TOPIC_AI_QUIZ_GENERATED = "ai-quiz-generated-topic";

    private final QuizQuestionRepository quizQuestionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes an AI-generated quiz payload and persists questions to MySQL.
     *
     * <p>Steps:
     * <ol>
     *   <li>Parse the JSON payload and extract {@code courseId}.</li>
     *   <li>Delete all existing AI-generated questions for that course (idempotent overwrite).</li>
     *   <li>Map each question node to a {@link QuizQuestion} with {@code isAiGenerated = true}.</li>
     *   <li>Batch-save all questions using {@code saveAll()} for performance.</li>
     * </ol>
     *
     * @param message raw JSON string from the Kafka topic
     */
    @KafkaListener(topics = TOPIC_AI_QUIZ_GENERATED, groupId = "course-service-group")
    @Transactional
    public void consumeAiQuizGenerated(final String message) {
        try {
            final JsonNode payload = objectMapper.readTree(message);
            final Long courseId = payload.get("courseId").asLong();

            log.info("Received AI_QUIZ_GENERATED event for courseId={}", courseId);

            // Idempotent overwrite: delete stale AI questions before inserting fresh ones.
            // This handles both Kafka re-delivery and course re-upload scenarios.
            quizQuestionRepository.deleteAiGeneratedByCourseId(courseId);
            log.debug("Deleted existing AI questions for courseId={} before fresh insert", courseId);

            final JsonNode questionsNode = payload.get("questions");
            if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
                log.warn("AI_QUIZ_GENERATED payload for courseId={} contains no questions. Skipping.", courseId);
                return;
            }

            final List<QuizQuestion> questions = parseQuestions(courseId, questionsNode);

            if (questions.isEmpty()) {
                log.warn("No valid questions could be parsed for courseId={}. Skipping save.", courseId);
                return;
            }

            // Batch save for performance — avoids N individual INSERT statements
            quizQuestionRepository.saveAll(questions);
            log.info("Saved {} AI-generated quiz questions for courseId={}", questions.size(), courseId);

        } catch (Exception e) {
            log.error("Failed to process AI_QUIZ_GENERATED event: {}", e.getMessage(), e);
            // Let the exception propagate for Kafka's retry / DLQ mechanism
            throw new RuntimeException("AI quiz ingestion failed: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Parses the JSON questions array into a list of {@link QuizQuestion} entities.
     * Skips malformed entries individually to maximize partial tolerance.
     *
     * @param courseId      the course these questions belong to
     * @param questionsNode the JSON array node from the Kafka payload
     * @return list of valid, mapped quiz questions with {@code isAiGenerated = true}
     */
    private List<QuizQuestion> parseQuestions(final Long courseId, final JsonNode questionsNode) {
        final List<QuizQuestion> questions = new ArrayList<>();

        for (final JsonNode node : questionsNode) {
            try {
                final QuizQuestion question = mapNodeToQuestion(courseId, node);
                questions.add(question);
            } catch (Exception e) {
                // Log and skip malformed questions rather than failing the entire batch
                log.warn("Skipping malformed question node for courseId={}: {}", courseId, e.getMessage());
            }
        }

        return questions;
    }

    /**
     * Maps a single JSON question node to a {@link QuizQuestion} JPA entity.
     *
     * @param courseId the owning course
     * @param node     the JSON node representing one question
     * @return fully populated QuizQuestion with isAiGenerated = true
     * @throws IllegalArgumentException if mandatory fields are missing or invalid
     */
    private QuizQuestion mapNodeToQuestion(final Long courseId, final JsonNode node) {
        final String questionText = requireText(node, "questionText", courseId);
        final String correctAnswer = requireText(node, "correctAnswer", courseId);
        final String concept = node.path("concept").asText("General");

        // Parse lessonId — optional, may be null for course-level questions
        final Long lessonId = node.has("lessonId") && !node.path("lessonId").isNull()
                ? node.path("lessonId").asLong()
                : null;

        // Parse quiz type enum — default to PRE_QUIZ if missing or invalid
        final QuizType quizType = parseQuizType(node.path("quizType").asText("PRE_QUIZ"));

        // Parse difficulty rating enum — nullable if AI omits it
        final DifficultyLevel difficultyRating = parseDifficultyLevel(
                node.path("difficultyRating").asText(""));

        // Convert the JSON options array to a Jackson ArrayNode for JSON column storage
        final ArrayNode optionsNode = objectMapper.createArrayNode();
        node.path("options").forEach(opt -> optionsNode.add(opt.asText()));

        if (optionsNode.size() < 2) {
            throw new IllegalArgumentException(
                    "Question has fewer than 2 options: '" + questionText + "'");
        }

        return QuizQuestion.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .concept(concept)
                .questionText(questionText)
                .options(optionsNode)
                .correctAnswer(correctAnswer)
                .quizType(quizType)
                .difficultyRating(difficultyRating)
                .isAiGenerated(true)  // ← explicit: all questions from this consumer are AI-generated
                .build();
    }

    /**
     * Extracts a mandatory non-blank text field from a JSON node.
     *
     * @param node     the JSON node
     * @param field    field name
     * @param courseId used in the error message for traceability
     * @return the non-blank text value
     * @throws IllegalArgumentException if the field is missing or blank
     */
    private String requireText(final JsonNode node, final String field, final Long courseId) {
        final String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Mandatory field '" + field + "' is missing for courseId=" + courseId);
        }
        return value;
    }

    /**
     * Safely parses a quiz type string into the {@link QuizType} enum.
     * Defaults to {@code PRE_QUIZ} if the value is unrecognized to avoid data loss.
     *
     * @param value the raw string value
     * @return the resolved QuizType enum constant
     */
    private QuizType parseQuizType(final String value) {
        try {
            return QuizType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown quizType '{}', defaulting to PRE_QUIZ", value);
            return QuizType.PRE_QUIZ;
        }
    }

    /**
     * Safely parses a difficulty rating string into the {@link DifficultyLevel} enum.
     * Returns {@code null} if the value is blank or unrecognized.
     *
     * @param value the raw string value
     * @return the resolved DifficultyLevel, or {@code null}
     */
    private DifficultyLevel parseDifficultyLevel(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DifficultyLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown difficultyRating '{}', setting null", value);
            return null;
        }
    }
}
