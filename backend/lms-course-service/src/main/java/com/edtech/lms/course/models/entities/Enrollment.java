package com.edtech.lms.course.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Enrollment Entity - Represents course assignments to employees
 * Tracks which employees are taking which courses, with deadlines and progress
 * Row-level multi-tenancy: company_id
 */
@Entity
@Table(name = "svc_course_enrollments", indexes = {
    @Index(name = "idx_enrollment_employee_id", columnList = "employee_id"),
    @Index(name = "idx_enrollment_course_id", columnList = "course_id"),
    @Index(name = "idx_enrollment_company_id", columnList = "company_id"),
    @Index(name = "idx_enrollment_status", columnList = "status"),
    @Index(name = "idx_enrollment_unique", columnList = "employee_id, course_id, company_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long enrollmentId;

    @Column(nullable = false)
    private String companyId;

    /**
     * Employee taking the course
     */
    @Column(nullable = false)
    private String employeeId;

    /**
     * Course being assigned
     */
    @Column(nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ASSIGNED;

    /**
     * Date course was assigned
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime assignedDate = LocalDateTime.now();

    /**
     * When the course must be completed by
     */
    @Column(nullable = true)
    private LocalDateTime deadline;

    /**
     * When the employee completed the course
     */
    @Column(nullable = true)
    private LocalDateTime completionDate;

    /**
     * Current progress percentage (0-100)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    /**
     * Latest score (if applicable)
     */
    @Column(nullable = true)
    private Double score;

    /**
     * AI-generated recommendations (stored as JSON)
     */
    @Column(columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode recommendations;

    /**
     * Per-lesson gating map written at PRE_QUIZ submission.
     * Maps lessonId → "RECOMMENDED" or "OPTIONAL" based on 3-factor engine:
     *   1. Incorrect response
     *   2. High hesitation (answer_changes > 2)
     *   3. High cognitive load (time_spent_ms > 15000)
     * Any single factor fires → lesson tagged RECOMMENDED.
     * Example: {"101": "RECOMMENDED", "202": "OPTIONAL", "303": "RECOMMENDED"}
     */
    @Column(columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode lessonGatingMap;

    /**
     * List of completed lesson IDs (stored as JSON array)
     */
    @Column(columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode completedLessons;

    /**
     * Score from PRE_QUIZ (% correct). Stored as baseline for uplift calculation.
     */
    @Column(nullable = true)
    private Double preQuizScore;

    /**
     * Score from POST_QUIZ (% correct). Stored after course completion.
     */
    @Column(nullable = true)
    private Double postQuizScore;

    /**
     * Computed uplift = postQuizScore - preQuizScore.
     * Represents the skill gain from completing the course.
     */
    @Column(nullable = true)
    private Double upliftPercent;

    /**
     * Per-concept uplift breakdown (stored as JSON):
     * {
     *   "conceptsGained": ["State Management", "Performance"],
     *   "noChange": ["UI Foundations"],
     *   "regression": [],
     *   "stillStruggling": ["Advanced Patterns"]
     * }
     */
    @Column(columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode upliftReport;

    /**
     * Track certificate approval workflow.
     * Allowed values: NOT_ELIGIBLE, ELIGIBLE, REQUESTED, APPROVED
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String certificateStatus = "NOT_ELIGIBLE";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
