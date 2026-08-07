package com.edtech.lms.course.services.impl;

import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.DifficultyLevel;
import com.edtech.lms.course.models.enums.QuizType;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiQuizKafkaConsumerTest — Unit tests for the AI quiz Kafka consumer in lms-course-service.
 *
 * <p>Covers:
 * <ul>
 *   <li>Happy path: valid payload → questions saved with isAiGenerated=true</li>
 *   <li>Idempotency: existing AI questions deleted before inserting new ones</li>
 *   <li>Partial tolerance: malformed questions skipped, valid ones saved</li>
 *   <li>Empty payload: no save() call made</li>
 *   <li>Enum handling: unknown quizType defaults to PRE_QUIZ; unknown difficulty → null</li>
 *   <li>Missing mandatory fields: question skipped with warning, no crash</li>
 *   <li>Kafka error: RuntimeException propagated for retry/DLQ</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiQuizKafkaConsumer Unit Tests")
class AiQuizKafkaConsumerTest {

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiQuizKafkaConsumer consumer;

    // =========================================================================
    // HAPPY PATH
    // =========================================================================

    @Nested
    @DisplayName("Happy Path")
    class HappyPathTests {

        @Test
        @DisplayName("Should save all valid questions with isAiGenerated=true")
        void shouldSaveQuestions_withAiGeneratedTrue() {
            // Given: a valid AI quiz payload with 2 questions
            final String message = buildValidPayload(42L, 2);

            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);

            // When
            consumer.consumeAiQuizGenerated(message);

            // Then
            verify(quizQuestionRepository).saveAll(captor.capture());
            final List<QuizQuestion> saved = captor.getValue();

            assertThat(saved).hasSize(2);
            assertThat(saved).allMatch(q -> Boolean.TRUE.equals(q.getIsAiGenerated()));
            assertThat(saved).allMatch(q -> q.getCourseId().equals(42L));
        }

        @Test
        @DisplayName("Should correctly parse quizType and difficultyRating enums")
        void shouldParseEnums_correctly() {
            final String message = """
                    {
                      "courseId": 10,
                      "questions": [{
                        "lessonId": 5,
                        "concept": "Spring IoC",
                        "questionText": "What is IoC?",
                        "options": ["A","B","C","D"],
                        "correctAnswer": "B",
                        "quizType": "POST_QUIZ",
                        "difficultyRating": "ADVANCED"
                      }]
                    }
                    """;
            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);

            consumer.consumeAiQuizGenerated(message);

            verify(quizQuestionRepository).saveAll(captor.capture());
            final QuizQuestion q = captor.getValue().get(0);
            assertThat(q.getQuizType()).isEqualTo(QuizType.POST_QUIZ);
            assertThat(q.getDifficultyRating()).isEqualTo(DifficultyLevel.ADVANCED);
            assertThat(q.getLessonId()).isEqualTo(5L);
        }
    }

    // =========================================================================
    // IDEMPOTENCY
    // =========================================================================

    @Nested
    @DisplayName("Idempotent Overwrite")
    class IdempotencyTests {

        @Test
        @DisplayName("Should delete existing AI questions BEFORE inserting new ones")
        void shouldDeleteExistingAiQuestions_beforeInsert() {
            final String message = buildValidPayload(99L, 1);

            consumer.consumeAiQuizGenerated(message);

            // The delete must come BEFORE saveAll — verified by InOrder
            final var order = inOrder(quizQuestionRepository);
            order.verify(quizQuestionRepository).deleteAiGeneratedByCourseId(99L);
            order.verify(quizQuestionRepository).saveAll(any());
        }
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should skip question with missing questionText — partial tolerance")
        void shouldSkipMalformedQuestion_missingQuestionText() {
            final String message = """
                    {
                      "courseId": 1,
                      "questions": [
                        {"questionText":"","correctAnswer":"A","options":["A","B","C","D"],"concept":"X","quizType":"PRE_QUIZ"},
                        {"questionText":"Valid Q?","correctAnswer":"B","options":["A","B","C","D"],"concept":"Y","quizType":"PRE_QUIZ"}
                      ]
                    }
                    """;
            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);

            consumer.consumeAiQuizGenerated(message);

            verify(quizQuestionRepository).saveAll(captor.capture());
            // Only 1 valid question saved, malformed one skipped
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getQuestionText()).isEqualTo("Valid Q?");
        }

        @Test
        @DisplayName("Should skip question with fewer than 2 options")
        void shouldSkipQuestion_withTooFewOptions() {
            final String message = """
                    {
                      "courseId": 2,
                      "questions": [
                        {"questionText":"Q?","correctAnswer":"A","options":["A"],"concept":"X","quizType":"PRE_QUIZ"}
                      ]
                    }
                    """;
            consumer.consumeAiQuizGenerated(message);

            // No questions pass validation → consumer early-exits before saveAll (see log: "Skipping save")
            verify(quizQuestionRepository).deleteAiGeneratedByCourseId(2L);
            verify(quizQuestionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should default unknown quizType to PRE_QUIZ")
        void shouldDefaultUnknownQuizType_toPreQuiz() {
            final String message = """
                    {
                      "courseId": 3,
                      "questions": [{
                        "questionText":"Q?","correctAnswer":"A",
                        "options":["A","B","C","D"],"concept":"X",
                        "quizType":"UNKNOWN_TYPE"
                      }]
                    }
                    """;
            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);

            consumer.consumeAiQuizGenerated(message);

            verify(quizQuestionRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getQuizType()).isEqualTo(QuizType.PRE_QUIZ);
        }

        @Test
        @DisplayName("Should set difficultyRating to null for unknown difficulty string")
        void shouldSetNullDifficulty_forUnknownValue() {
            final String message = """
                    {
                      "courseId": 4,
                      "questions": [{
                        "questionText":"Q?","correctAnswer":"A",
                        "options":["A","B","C","D"],"concept":"X",
                        "quizType":"PRE_QUIZ","difficultyRating":"SUPER_HARD"
                      }]
                    }
                    """;
            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);

            consumer.consumeAiQuizGenerated(message);

            verify(quizQuestionRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getDifficultyRating()).isNull();
        }

        @Test
        @DisplayName("Should do nothing when questions array is empty")
        void shouldDoNothing_whenQuestionsEmpty() {
            final String message = "{\"courseId\": 5, \"questions\": []}";

            consumer.consumeAiQuizGenerated(message);

            // Delete still runs, but saveAll should never be called with empty early-exit
            verify(quizQuestionRepository).deleteAiGeneratedByCourseId(5L);
            verify(quizQuestionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException on malformed JSON for Kafka retry/DLQ")
        void shouldThrowRuntimeException_onMalformedJson() {
            final String malformedJson = "THIS IS NOT JSON {{{";

            assertThatThrownBy(() -> consumer.consumeAiQuizGenerated(malformedJson))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI quiz ingestion failed");
        }
    }

    // =========================================================================
    // FIXTURE BUILDER
    // =========================================================================

    /**
     * Builds a valid AI quiz payload JSON string.
     *
     * @param courseId      the course ID
     * @param questionCount number of valid questions to include
     * @return JSON string matching the Kafka schema
     */
    private String buildValidPayload(final long courseId, final int questionCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"courseId\":").append(courseId).append(",\"questions\":[");
        for (int i = 0; i < questionCount; i++) {
            if (i > 0) sb.append(",");
            sb.append("""
                    {
                      "lessonId": %d,
                      "concept": "Concept %d",
                      "questionText": "What is concept %d?",
                      "options": ["A","B","C","D"],
                      "correctAnswer": "A",
                      "quizType": "PRE_QUIZ",
                      "difficultyRating": "BEGINNER"
                    }
                    """.formatted(i + 1, i + 1, i + 1));
        }
        sb.append("]}");
        return sb.toString();
    }
}
