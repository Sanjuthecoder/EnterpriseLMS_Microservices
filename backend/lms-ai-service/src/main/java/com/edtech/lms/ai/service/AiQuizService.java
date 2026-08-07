package com.edtech.lms.ai.service;

import com.edtech.lms.ai.client.GeminiClient;
import com.edtech.lms.ai.constant.AiConstants;
import com.edtech.lms.ai.entity.AiQuestion;
import com.edtech.lms.ai.entity.CourseAiContext;
import com.edtech.lms.ai.exception.AiProviderUnavailableException;
import com.edtech.lms.ai.exception.AiResourceNotFoundException;
import com.edtech.lms.ai.repository.CourseAiContextRepository;
import com.edtech.lms.ai.util.GeminiResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AiQuizService — Asynchronous AI quiz generation and Kafka publishing.
 *
 * <h2>Architectural role</h2>
 * <p>This service is a <em>Content Engine</em> only. It:
 * <ol>
 *   <li>Receives a trigger (via {@link CourseLifecycleConsumer}) to generate quizzes.</li>
 *   <li>Calls Google Gemini API with a structured prompt to get a JSON question array.</li>
 *   <li>Publishes the result to {@code ai-quiz-generated-topic}.</li>
 * </ol>
 *
 * <p>It does NOT score answers, manage sessions, or perform gating — those responsibilities
 * have been moved to {@code EmployeeCourseService} in {@code lms-course-service}.
 *
 * <h2>Idempotency</h2>
 * <p>An idempotency check on {@link CourseAiContext#getPreQuizQuestions()} prevents
 * redundant Gemini API calls and API quota burn on Kafka re-delivery.
 *
 * <h2>Chunking</h2>
 * <p>Courses with more than {@link AiConstants#LESSON_CHUNK_SIZE} lessons are split into
 * batches to avoid Gemini token-limit degradation. All chunks are accumulated before the
 * single {@code AI_QUIZ_GENERATED} Kafka message is published.
 *
 * <h2>Async execution</h2>
 * <p>{@link #generateAndPublishCourseQuizzes(Long)} is annotated {@code @Async} so the Kafka
 * consumer thread returns immediately, preventing consumer rebalancing timeout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuizService {

    private final GeminiClient geminiClient;
    private final CourseAiContextRepository courseAiContextRepository;
    private final GeminiResponseParser geminiResponseParser;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("classpath:templates/prompts.json")
    private org.springframework.core.io.Resource promptsResource;

    private String quizSystemInstruction;
    private String preQuizPromptTemplate;
    private String postQuizPromptTemplate;

    @jakarta.annotation.PostConstruct
    public void initPrompts() throws java.io.IOException {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(promptsResource.getInputStream());
        this.quizSystemInstruction = root.path("system-quiz").asText();
        this.preQuizPromptTemplate = root.path("pre-quiz").asText();
        this.postQuizPromptTemplate = root.path("post-quiz").asText();
    }

    // =========================================================================
    // ASYNC QUIZ GENERATION & PUBLISHING
    // =========================================================================

    /**
     * Generates PRE and POST quiz questions for a course using Gemini and publishes
     * the result to the {@code ai-quiz-generated-topic} for the course service to consume.
     *
     * <p><b>Idempotency:</b> if {@link CourseAiContext} already has quiz questions stored,
     * this method re-publishes the cached payload without calling Gemini again.
     *
     * <p><b>Chunking:</b> lessons are processed in batches of {@link AiConstants#LESSON_CHUNK_SIZE}
     * to avoid Gemini token-limit exhaustion on large courses.
     *
     * <p><b>Async:</b> annotated with {@code @Async} so the Kafka consumer thread that
     * triggers this call is immediately freed — preventing consumer rebalancing timeout.
     *
     * @param courseId the course to generate quizzes for
     */
    @Async
    public void generateAndPublishCourseQuizzes(final Long courseId) {
        log.info("Async quiz generation started for courseId={}", courseId);

        final CourseAiContext context = courseAiContextRepository.findByCourseId(courseId)
                .orElseThrow(() -> new AiResourceNotFoundException(
                        "Course context not found for courseId=" + courseId));

        // Idempotency guard: skip Gemini calls if questions already exist
        if (hasExistingQuizzes(context)) {
            log.info("Quizzes already exist for courseId={}, re-publishing cached payload.", courseId);
            publishQuizPayload(courseId, context.getPreQuizQuestions(), context.getPostQuizQuestions());
            return;
        }

        try {
            final List<String> lessons = context.getLessonSummaries();

            // Generate PRE questions in lesson chunks
            final List<AiQuestion> preQuestions = generateInChunks(lessons, preQuizPromptTemplate, "PRE_QUIZ");
            // Rate-limit delay between pre and post generation (Gemini 5 RPM free tier)
            sleepSafe(15_000);
            // Generate POST questions in lesson chunks
            final List<AiQuestion> postQuestions = generateInChunks(lessons, postQuizPromptTemplate, "POST_QUIZ");

            // Persist to MongoDB (so next time we can skip Gemini)
            context.setPreQuizQuestions(preQuestions);
            context.setPostQuizQuestions(postQuestions);
            courseAiContextRepository.save(context);

            // Publish single combined event to course service
            publishQuizPayload(courseId, preQuestions, postQuestions);

            log.info("Quiz generation complete for courseId={}: {} pre + {} post questions",
                    courseId, preQuestions.size(), postQuestions.size());

        } catch (Exception e) {
            log.error("Quiz generation failed for courseId={}: {}", courseId, e.getMessage(), e);
            // Persist whatever context exists so the admin can retry via manual endpoint
            courseAiContextRepository.save(context);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Checks whether the context already has both pre and post quiz questions populated.
     *
     * @param context the course AI context document
     * @return {@code true} if both pre and post quizzes are non-empty
     */
    private boolean hasExistingQuizzes(final CourseAiContext context) {
        return context.getPreQuizQuestions() != null && !context.getPreQuizQuestions().isEmpty()
                && context.getPostQuizQuestions() != null && !context.getPostQuizQuestions().isEmpty();
    }

    /**
     * Splits lessons into chunks and accumulates Gemini-generated questions for all chunks.
     *
     * <p>A 15-second delay is applied between chunks to respect the Gemini free-tier
     * rate limit (5 RPM). For large courses this means total generation time is proportional
     * to {@code Math.ceil(lessons.size() / LESSON_CHUNK_SIZE) * 15s}.
     *
     * @param lessons      all lesson context strings for the course
     * @param promptPrefix the quiz-type-specific prompt prefix (pre or post)
     * @param quizType     "PRE_QUIZ" or "POST_QUIZ" (used for logging only)
     * @return accumulated list of questions across all chunks
     */
    private List<AiQuestion> generateInChunks(
            final List<String> lessons, final String promptPrefix, final String quizType) {

        final List<AiQuestion> allQuestions = new ArrayList<>();
        final int totalLessons = lessons.size();
        final int chunkSize = AiConstants.LESSON_CHUNK_SIZE;

        for (int start = 0; start < totalLessons; start += chunkSize) {
            final int end = Math.min(start + chunkSize, totalLessons);
            final List<String> chunk = lessons.subList(start, end);

            log.debug("Generating {} questions for lessons {}-{} of {} (courseId chunk)",
                    quizType, start + 1, end, totalLessons);

            final String prompt = buildPrompt(promptPrefix, chunk);
            final List<AiQuestion> chunkQuestions = generateQuestionsWithRetry(prompt, quizType);
            allQuestions.addAll(chunkQuestions);

            // Apply rate-limit delay between chunks (not after the last chunk)
            if (end < totalLessons) {
                sleepSafe(15_000);
            }
        }

        return allQuestions;
    }

    /**
     * Publishes the full AI-generated quiz payload to the {@code ai-quiz-generated-topic}.
     *
     * <p>The payload JSON schema matches the contract expected by
     * {@code AiQuizKafkaConsumer} in {@code lms-course-service}:
     * <pre>{@code {"courseId": 42, "questions": [{...}, ...]}}</pre>
     *
     * @param courseId      the course identifier (Kafka message key)
     * @param preQuestions  list of PRE_QUIZ questions
     * @param postQuestions list of POST_QUIZ questions
     */
    private void publishQuizPayload(
            final Long courseId,
            final List<AiQuestion> preQuestions,
            final List<AiQuestion> postQuestions) {
        try {
            final ObjectNode payload = objectMapper.createObjectNode();
            payload.put("courseId", courseId);

            final ArrayNode questionsArray = objectMapper.createArrayNode();
            appendQuestionsToArray(questionsArray, preQuestions, "PRE_QUIZ");
            appendQuestionsToArray(questionsArray, postQuestions, "POST_QUIZ");
            payload.set("questions", questionsArray);

            final String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(AiConstants.TOPIC_AI_QUIZ_GENERATED, String.valueOf(courseId), json);
            log.info("Published AI_QUIZ_GENERATED event for courseId={} with {} total questions",
                    courseId, questionsArray.size());

        } catch (Exception e) {
            log.error("Failed to publish AI_QUIZ_GENERATED for courseId={}: {}", courseId, e.getMessage(), e);
        }
    }

    /**
     * Serialises a list of questions into the Kafka payload array node.
     *
     * @param array     the array node to append to
     * @param questions list of questions to append
     * @param quizType  "PRE_QUIZ" or "POST_QUIZ"
     */
    private void appendQuestionsToArray(
            final ArrayNode array, final List<AiQuestion> questions, final String quizType) {
        for (final AiQuestion q : questions) {
            final ObjectNode node = objectMapper.createObjectNode();
            if (q.getLinkedLessonId() != null) {
                node.put("lessonId", q.getLinkedLessonId());
            }
            node.put("concept", q.getConcept());
            node.put("questionText", q.getQuestionText());
            node.put("correctAnswer", q.getCorrectAnswer());
            node.put("quizType", quizType);

            final ArrayNode opts = objectMapper.createArrayNode();
            if (q.getOptions() != null) {
                q.getOptions().forEach(opts::add);
            }
            node.set("options", opts);

            array.add(node);
        }
    }

    /**
     * Builds the Gemini prompt for a specific chunk of lessons.
     *
     * @param promptPrefix the base prompt template
     * @param lessonChunk  the subset of lesson summaries for this chunk
     * @return the fully assembled prompt string
     */
    private String buildPrompt(final String promptPrefix, final List<String> lessonChunk) {
        final StringBuilder sb = new StringBuilder(promptPrefix).append("\n\nLessons:\n");
        lessonChunk.forEach(l -> sb.append("  - ").append(l).append("\n"));
        return sb.toString();
    }

    /**
     * Calls Gemini with retry logic and parses the response.
     *
     * @param prompt   the fully assembled prompt
     * @param quizType label for log messages
     * @return parsed list of AiQuestion objects
     */
    private List<AiQuestion> generateQuestionsWithRetry(final String prompt, final String quizType) {
        for (int attempt = 1; attempt <= AiConstants.MAX_AI_RETRY_COUNT; attempt++) {
            try {
                final String rawResponse = geminiClient.generateText(quizSystemInstruction, prompt);
                final List<AiQuestion> questions = geminiResponseParser.parseQuestions(rawResponse);
                if (questions == null || questions.isEmpty()) {
                    throw new AiProviderUnavailableException("Parsed " + quizType + " questions list is empty");
                }
                return questions;
            } catch (Exception e) {
                log.warn("{} generation attempt {}/{} failed: {}", quizType, attempt,
                        AiConstants.MAX_AI_RETRY_COUNT, e.getMessage());
                if (attempt == AiConstants.MAX_AI_RETRY_COUNT) {
                    throw new RuntimeException(quizType + " generation failed after "
                            + AiConstants.MAX_AI_RETRY_COUNT + " attempts", e);
                }
                sleepSafe(1000L * attempt);
            }
        }
        return List.of();
    }

    /**
     * Sleeps for the specified duration, handling interruption gracefully.
     *
     * @param millis duration in milliseconds
     */
    private void sleepSafe(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted during quiz generation.");
        }
    }
}
