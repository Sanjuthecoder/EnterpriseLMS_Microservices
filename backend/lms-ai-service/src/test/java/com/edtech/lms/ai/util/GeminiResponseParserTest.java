package com.edtech.lms.ai.util;

import com.edtech.lms.ai.entity.AiQuestion;
import com.edtech.lms.ai.exception.AiProviderUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * GeminiResponseParserTest — Tests for robust Gemini JSON parsing.
 *
 * Corner cases verified:
 * - Valid clean JSON array
 * - JSON wrapped in markdown code fences (Gemini's frequent behavior)
 * - Missing required fields (partial tolerance — skip bad questions)
 * - Empty response
 * - Completely unparseable response
 * - All questions malformed (throws exception)
 */
@DisplayName("GeminiResponseParser Unit Tests")
class GeminiResponseParserTest {

    private GeminiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new GeminiResponseParser(new ObjectMapper());
    }

    @Test
    @DisplayName("Should parse a clean valid JSON array of questions")
    void shouldParse_cleanJson() {
        // Given: A perfect response from Gemini
        final String rawJson = """
                [
                  {
                    "questionText": "What is Java?",
                    "options": ["A language", "A drink", "A library", "A framework"],
                    "correctAnswer": "A language",
                    "concept": "Java Basics"
                  }
                ]
                """;

        // When
        final List<AiQuestion> questions = parser.parseQuestions(rawJson);

        // Then
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getCorrectAnswer()).isEqualTo("A language");
        assertThat(questions.get(0).getConcept()).isEqualTo("Java Basics");
    }

    @Test
    @DisplayName("Should parse JSON wrapped in markdown code fences (Gemini frequently does this)")
    void shouldParse_markdownWrappedJson() {
        // Corner case: Gemini wraps its JSON in ```json ... ```
        final String rawJson = """
                ```json
                [
                  {
                    "questionText": "What is Spring Boot?",
                    "options": ["A framework", "A database", "An OS", "A language"],
                    "correctAnswer": "A framework",
                    "concept": "Spring"
                  }
                ]
                ```
                """;

        // When
        final List<AiQuestion> questions = parser.parseQuestions(rawJson);

        // Then: Successfully parsed despite markdown fences
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getQuestionText()).isEqualTo("What is Spring Boot?");
    }

    @Test
    @DisplayName("Should skip malformed questions and return valid ones (partial tolerance)")
    void shouldSkipMalformedQuestions_andReturnValid() {
        // Corner case: One valid, one missing correctAnswer (malformed)
        final String rawJson = """
                [
                  {
                    "questionText": "Valid question?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "concept": "Concept1"
                  },
                  {
                    "questionText": "",
                    "options": ["A", "B"],
                    "correctAnswer": "",
                    "concept": "Concept2"
                  }
                ]
                """;

        // When
        final List<AiQuestion> questions = parser.parseQuestions(rawJson);

        // Then: Only the valid question is returned
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getQuestionText()).isEqualTo("Valid question?");
    }

    @Test
    @DisplayName("Should throw AiProviderUnavailableException when response is null or empty")
    void shouldThrow_whenResponseIsEmpty() {
        // Corner case: Gemini returned empty string (network issue / quota exceeded)
        assertThatThrownBy(() -> parser.parseQuestions(""))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageContaining("empty response");

        assertThatThrownBy(() -> parser.parseQuestions(null))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("Should throw AiProviderUnavailableException when response is not JSON at all")
    void shouldThrow_whenResponseIsCompletelyUnparseable() {
        // Corner case: Gemini returned a plain English paragraph instead of JSON
        final String plainText = "I cannot generate quiz questions at this time. Please try again.";

        assertThatThrownBy(() -> parser.parseQuestions(plainText))
                .isInstanceOf(AiProviderUnavailableException.class);
    }

    @Test
    @DisplayName("Should throw AiProviderUnavailableException when ALL questions are malformed")
    void shouldThrow_whenAllQuestionsAreMalformed() {
        // Corner case: Gemini returned JSON but every entry is missing required fields
        final String rawJson = """
                [
                  { "concept": "Test" },
                  { "options": ["A", "B"] }
                ]
                """;

        assertThatThrownBy(() -> parser.parseQuestions(rawJson))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageContaining("no valid questions");
    }
}
