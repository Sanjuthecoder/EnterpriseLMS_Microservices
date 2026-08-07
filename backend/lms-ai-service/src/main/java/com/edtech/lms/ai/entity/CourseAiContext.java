package com.edtech.lms.ai.entity;

import com.edtech.lms.ai.constant.AiConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

import com.edtech.lms.ai.entity.AiQuestion;

/**
 * CourseAiContext — Stores the textual course content used as AI prompt context,
 * plus the pre-generated AI quiz questions (cached to avoid repeated Gemini API calls).
 *
 * <p>Populated by:
 * <ol>
 *   <li>{@code CourseLifecycleConsumer} — reacts to {@code course-published-topic} Kafka events.</li>
 *   <li>Admin endpoint POST {@code /api/v1/ai/admin/generate-missing-quizzes} — one-time backfill.</li>
 * </ol>
 *
 * <p>This is NOT a vector embedding store — it holds raw text injected into Gemini prompts.
 */
@Document(collection = AiConstants.COLLECTION_AI_COURSE_CONTEXT)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAiContext {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long courseId;

    private String courseTitle;

    private String courseDescription;

    private String difficultyLevel;

    /**
     * Flat list of lesson titles and descriptions used for quiz generation context.
     * Kept concise to fit within Gemini's context window.
     */
    private List<String> lessonSummaries;

    /**
     * Full concatenated context string passed directly to Gemini.
     * Regenerated whenever the course content changes.
     */
    private String fullContext;

    /**
     * Pre-generated AI questions for the pre-quiz to avoid calling Gemini for every employee.
     */
    private List<AiQuestion> preQuizQuestions;

    /**
     * Pre-generated AI questions for the post-quiz to avoid calling Gemini for every employee.
     */
    private List<AiQuestion> postQuizQuestions;

    @Builder.Default
    private LocalDateTime ingestedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
