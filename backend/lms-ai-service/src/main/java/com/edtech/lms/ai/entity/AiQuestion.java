package com.edtech.lms.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AiQuestion — A single AI-generated quiz question used as the shared value object
 * across the AI pipeline.
 *
 * <p>Persisted as an embedded document inside {@link CourseAiContext} (pre/post quiz lists)
 * and used by {@link com.edtech.lms.ai.util.GeminiResponseParser} to return parsed
 * Gemini results to {@link com.edtech.lms.ai.service.AiQuizService}.
 *
 * <p><b>Security note:</b> {@code correctAnswer} is stored server-side only and must never
 * be included in any API response sent to the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuestion {

    /** The concept/topic this question tests (e.g., "Dependency Injection") */
    private String concept;

    /** ID of the lesson this question is linked to; may be null for course-level questions */
    private Long linkedLessonId;

    /** The question stem shown to the learner */
    private String questionText;

    /** Exactly 4 answer options */
    private List<String> options;

    /**
     * The correct option text (must match one of {@link #options} exactly).
     * Server-side only — never sent to the frontend.
     */
    private String correctAnswer;
}
