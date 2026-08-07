package com.edtech.lms.ai.util;

import com.edtech.lms.ai.entity.AiQuestion;
import com.edtech.lms.ai.exception.AiProviderUnavailableException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * GeminiResponseParser — Parses raw JSON text from Gemini API into domain objects.
 *
 * Gemini sometimes wraps the JSON in markdown code fences (```json...```).
 * This parser strips those fences before attempting deserialization.
 *
 * Corner case handling:
 * - Markdown-wrapped responses: stripped before parsing.
 * - Partial responses: skips malformed questions instead of failing entirely.
 * - Empty responses: throws AiProviderUnavailableException.
 * - Missing fields: uses safe fallback values to avoid null pointers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Parses a Gemini raw response into a list of AiQuestion objects.
     *
     * @param rawResponse raw text from Gemini API (may be JSON or markdown-wrapped JSON)
     * @return list of parsed questions
     * @throws AiProviderUnavailableException if the response cannot be parsed at all
     */
    public List<AiQuestion> parseQuestions(final String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiProviderUnavailableException("Gemini returned an empty response for quiz generation.");
        }

        final String cleanedJson = stripMarkdownFences(rawResponse);

        try {
            final List<JsonNode> rawQuestions = objectMapper.readValue(
                    cleanedJson, new TypeReference<List<JsonNode>>() {});

            final List<AiQuestion> questions = new ArrayList<>();
            for (final JsonNode node : rawQuestions) {
                final AiQuestion question = parseQuestionNode(node);
                if (question != null) {
                    questions.add(question);
                }
            }

            if (questions.isEmpty()) {
                throw new AiProviderUnavailableException(
                        "Gemini returned a response but no valid questions could be parsed.");
            }

            return questions;
        } catch (AiProviderUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse Gemini quiz response: {}", ex.getMessage());
            throw new AiProviderUnavailableException(
                    "Failed to parse AI response into quiz questions. Raw response: "
                    + cleanedJson.substring(0, Math.min(cleanedJson.length(), 200)));
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Strips ```json ... ``` markdown code fences if present.
     * Gemini frequently wraps JSON in these fences in its text responses.
     */
    private String stripMarkdownFences(final String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length());
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length());
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - "```".length());
        }
        return cleaned.trim();
    }

    /**
     * Parses a single question JSON node into an AiQuestion.
     * Returns null and logs a warning if the node is malformed (partial tolerance).
     */
    private AiQuestion parseQuestionNode(final JsonNode node) {
        try {
            final String questionText = node.path("questionText").asText("");
            final String correctAnswer = node.path("correctAnswer").asText("");
            final String concept = node.path("concept").asText("General");
            
            Long linkedLessonId = null;
            if (node.has("linkedLessonId") && !node.path("linkedLessonId").isNull()) {
                linkedLessonId = node.path("linkedLessonId").asLong();
            }

            if (questionText.isBlank() || correctAnswer.isBlank()) {
                log.warn("Skipping malformed question node (missing questionText or correctAnswer)");
                return null;
            }

            final List<String> options = new ArrayList<>();
            node.path("options").forEach(opt -> options.add(opt.asText()));

            if (options.size() < 2) {
                log.warn("Skipping question with fewer than 2 options: {}", questionText);
                return null;
            }

            return AiQuestion.builder()
                    .concept(concept)
                    .linkedLessonId(linkedLessonId)
                    .questionText(questionText)
                    .options(options)
                    .correctAnswer(correctAnswer)
                    .build();
        } catch (Exception ex) {
            log.warn("Skipping unparseable question node: {}", ex.getMessage());
            return null;
        }
    }
}
