package com.edtech.lms.course.models.enums;

/**
 * QuizType Enum - Distinguishes the purpose of a quiz attempt
 *
 * PRE_QUIZ  - Taken before any module content is consumed.
 *             Purpose: Diagnostic. Detect knowledge gaps.
 *             Result: Drives the 3-factor gating engine → writes lessonGatingMap to Enrollment.
 *             Failing is expected and is NEVER shown negatively to the employee.
 *
 * POST_QUIZ - Taken after all RECOMMENDED modules are completed.
 *             Purpose: Measurement. Prove skill uplift.
 *             Result: Triggers uplift computation comparing pre vs post scores per concept.
 */
public enum QuizType {
    PRE_QUIZ,
    POST_QUIZ
}
