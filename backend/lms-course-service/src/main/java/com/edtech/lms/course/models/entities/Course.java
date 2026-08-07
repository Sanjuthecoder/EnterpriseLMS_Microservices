package com.edtech.lms.course.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.models.enums.DifficultyLevel;
import java.time.LocalDateTime;

/**
 * Course Entity - Represents courses created by content creators
 * Each course belongs to an organization (org_id for multi-tenancy)
 * Row-level multi-tenancy: org_id
 */
@Entity
@Table(name = "svc_course_courses", indexes = {
    @Index(name = "idx_course_org_id", columnList = "org_id"),
    @Index(name = "idx_course_creator_id", columnList = "creator_id"),
    @Index(name = "idx_course_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @Column(nullable = false)
    private String orgId;

    /**
     * Creator of the course (references User with role CREATOR)
     */
    @Column(nullable = false)
    private String creatorId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    /**
     * Total duration in minutes
     */
    @Column(nullable = true)
    private Integer durationMinutes;

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
