package com.edtech.lms.course.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.QuizType;

import java.util.List;

/**
 * QuizQuestionRepository — Data access for quiz questions in {@code svc_course_quiz_questions}.
 *
 * <p>Database operations only. No business logic permitted here.
 *
 * <p>Key query methods:
 * <ul>
 *   <li>{@link #findByCourseIdAndQuizTypeAndIsAiGenerated} — primary routing query used by
 *       {@code EmployeeCourseService} to serve AI or creator questions based on tier.</li>
 *   <li>{@link #deleteAiGeneratedByCourseId} — used by the AI quiz Kafka consumer to perform
 *       an idempotent overwrite, preventing duplicate AI questions on re-upload.</li>
 * </ul>
 */
@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    /**
     * Returns all questions for a given course, regardless of type or source.
     *
     * @param courseId the course identifier
     * @return list of all quiz questions for the course
     */
    List<QuizQuestion> findByCourseId(Long courseId);

    /**
     * Returns all questions associated with a specific lesson.
     *
     * @param lessonId the lesson identifier
     * @return list of questions linked to the lesson
     */
    List<QuizQuestion> findByLessonId(Long lessonId);

    /**
     * Returns questions filtered by quiz type only.
     * Used by legacy paths where tier routing is not required.
     *
     * @param courseId the course identifier
     * @param quizType PRE_QUIZ or POST_QUIZ
     * @return list of matching questions
     */
    List<QuizQuestion> findByCourseIdAndQuizType(Long courseId, QuizType quizType);

    /**
     * Primary smart-routing query. Returns questions filtered by both quiz type and source.
     *
     * <p>For Premium users: call with {@code isAiGenerated = true} to get AI questions,
     * then fall back to {@code false} if the list is empty (AI still generating).
     * For non-Premium users: always call with {@code isAiGenerated = false}.
     *
     * @param courseId      the course identifier
     * @param quizType      PRE_QUIZ or POST_QUIZ
     * @param isAiGenerated {@code true} for AI-generated, {@code false} for creator-authored
     * @return list of matching questions
     */
    List<QuizQuestion> findByCourseIdAndQuizTypeAndIsAiGenerated(
            Long courseId, QuizType quizType, Boolean isAiGenerated);

    /**
     * Idempotent delete of all AI-generated questions for a course.
     * Called by the AI quiz Kafka consumer <em>before</em> inserting the new payload.
     * This prevents duplicate AI questions when the Kafka event is redelivered or
     * when a creator re-uploads a course.
     *
     * @param courseId the course to clear AI questions for
     */
    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.courseId = :courseId AND q.isAiGenerated = true")
    void deleteAiGeneratedByCourseId(@Param("courseId") Long courseId);
}
