package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.edtech.lms.course.models.enums.QuizType;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeCourseService.
 *
 * Covers:
 * - Dashboard retrieval
 * - Pre-quiz question fetch (happy + already submitted)
 * - Pre-quiz submission + 3-factor gating engine
 * - Lesson completion + progress calculation
 * - Post-quiz submission + uplift calculation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeCourseService Unit Tests")
class EmployeeCourseServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseStructureRepository courseStructureRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    @Mock private org.springframework.web.client.RestTemplate restTemplate;

    @InjectMocks private EmployeeCourseService employeeCourseService;

    private Enrollment freshEnrollment;
    private Enrollment completedPreQuizEnrollment;
    private Course course;
    private QuizQuestion q1;
    private QuizQuestion q2;

    @BeforeEach
    void setUp() throws Exception {
        course = new Course();
        course.setCourseId(100L);
        course.setTitle("Java Fundamentals");
        course.setStatus(CourseStatus.PUBLISHED);

        freshEnrollment = new Enrollment();
        freshEnrollment.setEnrollmentId(1L);
        freshEnrollment.setEmployeeId("1");
        freshEnrollment.setCompanyId("1");
        freshEnrollment.setCourseId(100L);
        freshEnrollment.setStatus(EnrollmentStatus.ASSIGNED);
        freshEnrollment.setProgressPercentage(0);
        freshEnrollment.setLessonGatingMap(null); // Pre-quiz not taken

        completedPreQuizEnrollment = new Enrollment();
        completedPreQuizEnrollment.setEnrollmentId(2L);
        completedPreQuizEnrollment.setEmployeeId("1");
        completedPreQuizEnrollment.setCompanyId("1");
        completedPreQuizEnrollment.setCourseId(100L);
        completedPreQuizEnrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        // Set a non-null gating map to simulate pre-quiz already done
        ObjectMapper mapper = new ObjectMapper();
        completedPreQuizEnrollment.setLessonGatingMap(mapper.readTree("{\"1\":\"RECOMMENDED\"}"));
        completedPreQuizEnrollment.setPreQuizScore(60.0);

        q1 = new QuizQuestion();
        q1.setQuestionId(1L);
        q1.setCourseId(100L);
        q1.setLessonId(1L);
        q1.setConcept("Variables");
        q1.setQuestionText("What is a variable?");
        q1.setCorrectAnswer("A");
        q1.setQuizType(QuizType.PRE_QUIZ);

        q2 = new QuizQuestion();
        q2.setQuestionId(2L);
        q2.setCourseId(100L);
        q2.setLessonId(2L);
        q2.setConcept("Loops");
        q2.setQuestionText("What is a for loop?");
        q2.setCorrectAnswer("B");
        q2.setQuizType(QuizType.PRE_QUIZ);
    }

    // =========================================================================
    // PRE-QUIZ
    // =========================================================================

    @Test
    @DisplayName("getPreQuizQuestions — should return sanitized questions without correct answers")
    void getPreQuiz_returnsQuestionsWithoutAnswers() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false))
                .thenReturn(List.of(q1, q2));

        List<Map<String, Object>> questions = employeeCourseService.getPreQuizQuestions("1", "1", 100L, "FREE");

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0)).containsKey("questionId");
        assertThat(questions.get(0)).containsKey("concept");
        assertThat(questions.get(0)).doesNotContainKey("correctAnswer");
    }

    @Test
    @DisplayName("getPreQuizQuestions — should throw if pre-quiz already completed")
    void getPreQuiz_alreadySubmitted() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(completedPreQuizEnrollment));

        assertThatThrownBy(() -> employeeCourseService.getPreQuizQuestions("1", "1", 100L, "FREE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    // =========================================================================
    // 3-FACTOR GATING ENGINE
    // =========================================================================

    @Test
    @DisplayName("submitPreQuiz — correct answer, no hesitation → OPTIONAL gating")
    void submitPreQuiz_correctAnswer_gatesToOptional() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizType(100L, QuizType.PRE_QUIZ))
                .thenReturn(List.of(q1));
        when(enrollmentRepository.save(any())).thenReturn(freshEnrollment);

        List<Map<String, Object>> answers = List.of(Map.of(
                "questionId", 1, "answer", "A", "answerChanges", 0, "timeSpentMs", 5000L));

        Map<String, Object> result = employeeCourseService.submitPreQuiz("1", "1", 100L, answers);

        assertThat(result).containsKey("lessonGatingMap");
        @SuppressWarnings("unchecked")
        Map<String, String> gatingMap = (Map<String, String>) result.get("lessonGatingMap");
        assertThat(gatingMap).containsValue("OPTIONAL");
        
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("submitPreQuiz — wrong answer → RECOMMENDED gating (factor 1)")
    void submitPreQuiz_wrongAnswer_gatesToRecommended() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizType(100L, QuizType.PRE_QUIZ))
                .thenReturn(List.of(q1));
        when(enrollmentRepository.save(any())).thenReturn(freshEnrollment);

        List<Map<String, Object>> answers = List.of(Map.of(
                "questionId", 1, "answer", "D", "answerChanges", 0, "timeSpentMs", 3000L));

        Map<String, Object> result = employeeCourseService.submitPreQuiz("1", "1", 100L, answers);

        @SuppressWarnings("unchecked")
        Map<String, String> gatingMap = (Map<String, String>) result.get("lessonGatingMap");
        assertThat(gatingMap).containsValue("RECOMMENDED");
    }

    @Test
    @DisplayName("submitPreQuiz — high hesitation (answerChanges > 2) → RECOMMENDED even if correct")
    void submitPreQuiz_highHesitation_gatesToRecommended() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizType(100L, QuizType.PRE_QUIZ))
                .thenReturn(List.of(q1));
        when(enrollmentRepository.save(any())).thenReturn(freshEnrollment);

        List<Map<String, Object>> answers = List.of(Map.of(
                "questionId", 1, "answer", "A", "answerChanges", 5, "timeSpentMs", 3000L));

        Map<String, Object> result = employeeCourseService.submitPreQuiz("1", "1", 100L, answers);

        @SuppressWarnings("unchecked")
        Map<String, String> gatingMap = (Map<String, String>) result.get("lessonGatingMap");
        assertThat(gatingMap).containsValue("RECOMMENDED");
    }

    @Test
    @DisplayName("submitPreQuiz — cognitive overload (timeSpentMs > 15000) → RECOMMENDED even if correct")
    void submitPreQuiz_cognitiveOverload_gatesToRecommended() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizType(100L, QuizType.PRE_QUIZ))
                .thenReturn(List.of(q1));
        when(enrollmentRepository.save(any())).thenReturn(freshEnrollment);

        List<Map<String, Object>> answers = List.of(Map.of(
                "questionId", 1, "answer", "A", "answerChanges", 0, "timeSpentMs", 20000L));

        Map<String, Object> result = employeeCourseService.submitPreQuiz("1", "1", 100L, answers);

        @SuppressWarnings("unchecked")
        Map<String, String> gatingMap = (Map<String, String>) result.get("lessonGatingMap");
        assertThat(gatingMap).containsValue("RECOMMENDED");
    }

    // =========================================================================
    // POST-QUIZ UPLIFT
    // =========================================================================

    @Test
    @DisplayName("submitPostQuiz — should compute uplift = postScore - preScore")
    void submitPostQuiz_computesUplift() {
        QuizQuestion pq = new QuizQuestion();
        pq.setQuestionId(10L);
        pq.setCourseId(100L);
        pq.setConcept("Loops");
        pq.setCorrectAnswer("B");
        pq.setQuizType(QuizType.POST_QUIZ);
        pq.setIsAiGenerated(false);

        completedPreQuizEnrollment.setPreQuizScore(40.0);

        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(completedPreQuizEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.POST_QUIZ, false))
                .thenReturn(List.of(pq));
        when(enrollmentRepository.save(any())).thenReturn(completedPreQuizEnrollment);

        List<Map<String, Object>> answers = List.of(Map.of("questionId", 10, "answer", "B"));

        Map<String, Object> result = employeeCourseService.submitPostQuiz("1", "1", 100L, "FREE", answers);

        assertThat(result.get("postQuizScore")).isEqualTo(100.0);
        assertThat(result.get("preQuizScore")).isEqualTo(40.0);
        assertThat(result.get("upliftPercent")).isEqualTo(60.0); // 100 - 40
    }

    // =========================================================================
    // ENROLLMENT NOT FOUND
    // =========================================================================

    @Test
    @DisplayName("Any method — should throw if employee not enrolled")
    void anyMethod_notEnrolled_throws() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("99", 100L, "1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeCourseService.getPreQuizQuestions("99", "1", 100L, "FREE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // CERTIFICATES
    // =========================================================================

    @Test
    @DisplayName("requestCertificate — should succeed if ELIGIBLE")
    void requestCertificate_eligible_success() {
        completedPreQuizEnrollment.setCertificateStatus("ELIGIBLE");
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(completedPreQuizEnrollment));
        when(enrollmentRepository.save(any())).thenReturn(completedPreQuizEnrollment);

        Map<String, Object> result = employeeCourseService.requestCertificate("1", "1", 100L);
        assertThat(result.get("certificateStatus")).isEqualTo("REQUESTED");
        assertThat(completedPreQuizEnrollment.getCertificateStatus()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("requestCertificate — should throw if not ELIGIBLE")
    void requestCertificate_notEligible_throws() {
        completedPreQuizEnrollment.setCertificateStatus("NOT_ELIGIBLE");
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(completedPreQuizEnrollment));

        assertThatThrownBy(() -> employeeCourseService.requestCertificate("1", "1", 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot request certificate");
    }

    // =========================================================================
    // SMART ROUTING — isAiGenerated tier logic
    // =========================================================================

    @Test
    @DisplayName("getPreQuizQuestions — Premium company receives AI questions when available")
    void getPreQuiz_premium_servesAiQuestions() {
        // Given: Premium company (companyId contains "_premium")
        final String premiumCompanyId = "500_premium";
        final Enrollment enrollment = buildFreshEnrollment("2", premiumCompanyId);
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("2", 100L, premiumCompanyId))
                .thenReturn(Optional.of(enrollment));

        final QuizQuestion aiQuestion = buildAiQuestion(99L, QuizType.PRE_QUIZ);
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, true))
                .thenReturn(List.of(aiQuestion));

        // When
        final List<Map<String, Object>> result =
                employeeCourseService.getPreQuizQuestions("2", premiumCompanyId, 100L, "PREMIUM");

        // Then: AI questions served, creator questions never queried
        assertThat(result).hasSize(1);
        verify(quizQuestionRepository, times(1))
                .findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, true);
        verify(quizQuestionRepository, never())
                .findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false);
    }

    @Test
    @DisplayName("getPreQuizQuestions — Premium company falls back to creator questions when AI is pending")
    void getPreQuiz_premium_fallsBackToCreator_whenAiNotReady() {
        // Race condition: Premium user starts course before AI finishes generating
        final String premiumCompanyId = "500_premium";
        final Enrollment enrollment = buildFreshEnrollment("3", premiumCompanyId);
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("3", 100L, premiumCompanyId))
                .thenReturn(Optional.of(enrollment));

        // AI questions NOT ready yet
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, true))
                .thenReturn(List.of());
        // Creator fallback available
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false))
                .thenReturn(List.of(q1));

        // When
        final List<Map<String, Object>> result =
                employeeCourseService.getPreQuizQuestions("3", premiumCompanyId, 100L, "PREMIUM");

        // Then: graceful fallback to creator questions — user is not blocked
        assertThat(result).hasSize(1);
        verify(quizQuestionRepository, times(1))
                .findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false);
    }

    @Test
    @DisplayName("getPreQuizQuestions — Non-Premium company always receives creator questions")
    void getPreQuiz_nonPremium_alwaysServesCreatorQuestions() {
        // Standard (non-premium) company
        final String stdCompanyId = "1";
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, stdCompanyId))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false))
                .thenReturn(List.of(q1, q2));

        // When
        final List<Map<String, Object>> result =
                employeeCourseService.getPreQuizQuestions("1", stdCompanyId, 100L, "FREE");

        // Then: creator questions served, AI path never queried
        assertThat(result).hasSize(2);
        verify(quizQuestionRepository, never())
                .findByCourseIdAndQuizTypeAndIsAiGenerated(eq(100L), eq(QuizType.PRE_QUIZ), eq(true));
    }

    @Test
    @DisplayName("getPreQuizQuestions — returns empty list gracefully when no questions exist (no crash)")
    void getPreQuiz_returnsEmptyList_whenNoQuestionsExist() {
        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId("1", 100L, "1"))
                .thenReturn(Optional.of(freshEnrollment));
        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(100L, QuizType.PRE_QUIZ, false))
                .thenReturn(List.of());

        // Should return empty list, not throw (frontend handles empty state gracefully)
        assertThatCode(() -> employeeCourseService.getPreQuizQuestions("1", "1", 100L, "FREE"))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Enrollment buildFreshEnrollment(final String employeeId, final String companyId) {
        final Enrollment e = new Enrollment();
        e.setEnrollmentId(99L);
        e.setEmployeeId(employeeId);
        e.setCompanyId(companyId);
        e.setCourseId(100L);
        e.setStatus(EnrollmentStatus.ASSIGNED);
        e.setProgressPercentage(0);
        e.setLessonGatingMap(null);
        return e;
    }

    private QuizQuestion buildAiQuestion(final Long questionId, final QuizType quizType) {
        final QuizQuestion q = new QuizQuestion();
        q.setQuestionId(questionId);
        q.setCourseId(100L);
        q.setLessonId(1L);
        q.setConcept("AI Concept");
        q.setQuestionText("What is the AI answer?");
        q.setCorrectAnswer("A");
        q.setQuizType(quizType);
        q.setIsAiGenerated(true);
        return q;
    }
}

