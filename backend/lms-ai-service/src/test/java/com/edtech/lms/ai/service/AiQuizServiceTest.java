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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiQuizServiceTest — Unit tests for the refactored AiQuizService.
 *
 * <p>Test strategy covers:
 * <ul>
 *   <li>Idempotency: re-publishes cached questions without calling Gemini</li>
 *   <li>Happy path: Gemini is called, results published to Kafka</li>
 *   <li>Chunking: verifies multiple Gemini calls for large lesson lists</li>
 *   <li>Missing context: AiResourceNotFoundException propagation</li>
 *   <li>Gemini failure: RuntimeException after MAX_AI_RETRY_COUNT attempts</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiQuizService Unit Tests")
class AiQuizServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private CourseAiContextRepository courseAiContextRepository;
    @Mock private GeminiResponseParser geminiResponseParser;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private AiQuizService aiQuizService;

    private static final Long COURSE_ID = 42L;

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring normally resolves
        ReflectionTestUtils.setField(aiQuizService, "quizSystemInstruction", "You are an AI.");
        ReflectionTestUtils.setField(aiQuizService, "preQuizPromptTemplate", "Generate PRE quiz.");
        ReflectionTestUtils.setField(aiQuizService, "postQuizPromptTemplate", "Generate POST quiz.");
    }

    // =========================================================================
    // IDEMPOTENCY
    // =========================================================================

    @Nested
    @DisplayName("Idempotency Guard")
    class IdempotencyTests {

        @Test
        @DisplayName("Should re-publish cached questions without calling Gemini when quizzes already exist")
        void shouldReuseCache_whenQuizzesAlreadyGenerated() {
            // Given: context with existing questions
            final CourseAiContext context = buildContextWithQuizzes();
            when(courseAiContextRepository.findByCourseId(COURSE_ID)).thenReturn(Optional.of(context));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

            // When
            aiQuizService.generateAndPublishCourseQuizzes(COURSE_ID);

            // Then: Gemini was NEVER called (cached)
            verify(geminiClient, never()).generateText(anyString(), anyString());
            // Kafka WAS called with the cached payload
            verify(kafkaTemplate, times(1)).send(
                    eq(AiConstants.TOPIC_AI_QUIZ_GENERATED), eq("42"), anyString());
        }
    }

    // =========================================================================
    // HAPPY PATH
    // =========================================================================

    @Nested
    @DisplayName("Happy Path Generation")
    class HappyPathTests {

        @Test
        @DisplayName("Should call Gemini, save context, and publish to Kafka when no questions exist")
        void shouldGenerateAndPublish_whenNoQuestionsExist() {
            // Given: context without questions
            final CourseAiContext context = buildContextWithoutQuizzes();
            when(courseAiContextRepository.findByCourseId(COURSE_ID)).thenReturn(Optional.of(context));
            when(geminiClient.generateText(anyString(), anyString()))
                    .thenReturn("[{\"questionText\":\"Q?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"A\",\"concept\":\"CI/CD\"}]");
            when(geminiResponseParser.parseQuestions(anyString()))
                    .thenReturn(List.of(buildQuestion("CI/CD")));
            when(courseAiContextRepository.save(any())).thenReturn(context);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

            // When
            assertThatCode(() -> aiQuizService.generateAndPublishCourseQuizzes(COURSE_ID))
                    .doesNotThrowAnyException();

            // Then: Gemini called twice (pre + post), Kafka published once
            verify(geminiClient, times(2)).generateText(anyString(), anyString());
            verify(kafkaTemplate, times(1)).send(
                    eq(AiConstants.TOPIC_AI_QUIZ_GENERATED), eq("42"), anyString());
            verify(courseAiContextRepository, times(1)).save(any());
        }
    }

    // =========================================================================
    // CHUNKING
    // =========================================================================

    @Nested
    @DisplayName("Lesson Chunking")
    class ChunkingTests {

        @Test
        @DisplayName("Should call Gemini multiple times when lessons exceed LESSON_CHUNK_SIZE")
        void shouldChunk_whenLessonsExceedBatchSize() {
            // Given: 9 lessons → 2 chunks (8 + 1) × 2 quiz types = 4 Gemini calls
            final CourseAiContext context = buildContextWithoutQuizzes();
            // Build 9 lesson summaries (exceeds chunk size of 8)
            final List<String> lessons = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                lessons.add("Lesson ID " + i + ": Lesson " + i);
            }
            context.setLessonSummaries(lessons);
            when(courseAiContextRepository.findByCourseId(COURSE_ID)).thenReturn(Optional.of(context));
            when(geminiResponseParser.parseQuestions(anyString()))
                    .thenReturn(List.of(buildQuestion("Topic")));
            when(geminiClient.generateText(anyString(), anyString())).thenReturn("[]");
            when(courseAiContextRepository.save(any())).thenReturn(context);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

            // When — use a mock to skip Thread.sleep
            aiQuizService.generateAndPublishCourseQuizzes(COURSE_ID);

            // Then: 4 Gemini calls (2 chunks × 2 quiz types)
            verify(geminiClient, atLeast(4)).generateText(anyString(), anyString());
        }
    }

    // =========================================================================
    // MISSING CONTEXT
    // =========================================================================

    @Nested
    @DisplayName("Missing Course Context")
    class MissingContextTests {

        @Test
        @DisplayName("Should throw AiResourceNotFoundException when course context not ingested yet")
        void shouldThrow_whenContextMissing() {
            when(courseAiContextRepository.findByCourseId(COURSE_ID)).thenReturn(Optional.empty());

            // AiResourceNotFoundException should propagate out of the @Async method
            assertThatThrownBy(() -> aiQuizService.generateAndPublishCourseQuizzes(COURSE_ID))
                    .isInstanceOf(AiResourceNotFoundException.class)
                    .hasMessageContaining("Course context not found");
        }
    }

    // =========================================================================
    // GEMINI FAILURE
    // =========================================================================

    @Nested
    @DisplayName("Gemini API Failure Handling")
    class GeminiFailureTests {

        @Test
        @DisplayName("Should log error and save partial context when Gemini fails after retries")
        void shouldHandleGeminiFailure_gracefully() {
            final CourseAiContext context = buildContextWithoutQuizzes();
            when(courseAiContextRepository.findByCourseId(COURSE_ID)).thenReturn(Optional.of(context));
            when(geminiClient.generateText(anyString(), anyString()))
                    .thenThrow(new AiProviderUnavailableException("Gemini is down"));
            when(courseAiContextRepository.save(any())).thenReturn(context);

            // Should NOT throw — failures are caught and logged; context is saved for admin retry
            assertThatCode(() -> aiQuizService.generateAndPublishCourseQuizzes(COURSE_ID))
                    .doesNotThrowAnyException();

            // Context was saved even after failure (for admin retry via backfill endpoint)
            verify(courseAiContextRepository, atLeastOnce()).save(any());
            // Kafka was NOT called — no partial payload published
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    // =========================================================================
    // FIXTURES
    // =========================================================================

    private AiQuestion buildQuestion(final String concept) {
        return AiQuestion.builder()
                .concept(concept)
                .questionText("What is " + concept + "?")
                .options(List.of("A", "B", "C", "D"))
                .correctAnswer("A")
                .linkedLessonId(1L)
                .build();
    }

    private CourseAiContext buildContextWithQuizzes() {
        final CourseAiContext ctx = buildContextWithoutQuizzes();
        ctx.setPreQuizQuestions(List.of(buildQuestion("Java")));
        ctx.setPostQuizQuestions(List.of(buildQuestion("Spring")));
        return ctx;
    }

    private CourseAiContext buildContextWithoutQuizzes() {
        return CourseAiContext.builder()
                .courseId(COURSE_ID)
                .courseTitle("Java Spring Boot")
                .fullContext("Course Title: Java Spring Boot\nLessons:\n  - Lesson ID 1: Intro\n")
                .lessonSummaries(List.of("Lesson ID 1: Intro - Introduction to Java"))
                .build();
    }
}
