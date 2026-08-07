package com.edtech.lms.course.services.impl;

import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.edtech.lms.course.models.enums.QuizType;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.edtech.lms.course.services.EmployeeCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class EmployeeCourseServiceQuizRoutingTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EmployeeCourseService employeeCourseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPreQuiz_PremiumUser_ReturnsAiQuestions() {
        String employeeId = "101";
        String companyId = "2";
        Long courseId = 60002L;
        String subscriptionTier = "PREMIUM";

        Enrollment enrollment = new Enrollment();
        enrollment.setEmployeeId(employeeId);
        enrollment.setCompanyId(companyId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(EnrollmentStatus.ASSIGNED);

        when(enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId))
                .thenReturn(Optional.of(enrollment));

        QuizQuestion aiQuestion = new QuizQuestion();
        aiQuestion.setQuestionId(1L);
        aiQuestion.setCourseId(courseId);
        aiQuestion.setQuizType(QuizType.PRE_QUIZ);
        aiQuestion.setIsAiGenerated(true);
        aiQuestion.setQuestionText("AI Question");

        when(quizQuestionRepository.findByCourseIdAndQuizTypeAndIsAiGenerated(courseId, QuizType.PRE_QUIZ, true))
                .thenReturn(List.of(aiQuestion));

        List<Map<String, Object>> result = employeeCourseService.getPreQuizQuestions(employeeId, companyId, courseId, subscriptionTier);

        assertEquals(1, result.size());
        assertEquals("AI Question", result.get(0).get("questionText"));
    }
}
