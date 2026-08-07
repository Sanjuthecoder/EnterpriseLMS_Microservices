package com.edtech.lms.course.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.course.models.enums.ContentType;
import java.time.LocalDateTime;

/**
 * CourseStructure Entity - Represents lessons/modules within a course
 * Defines the sequence and structure of course content
 */
@Entity
@Table(name = "svc_course_course_structure", indexes = {
    @Index(name = "idx_lesson_course_id", columnList = "course_id"),
    @Index(name = "idx_lesson_seq", columnList = "course_id, seq_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonId;

    @Column(nullable = false)
    private Long courseId;

    /**
     * Sequence order within the course (1, 2, 3, ...)
     */
    @Column(nullable = false)
    private Integer seqOrder;

    /**
     * Type of content: VIDEO, PDF, TEXT, QUIZ, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType lessonType;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String contentUrl;

    /**
     * Duration in minutes
     */
    @Column(nullable = true)
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String moduleTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

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
