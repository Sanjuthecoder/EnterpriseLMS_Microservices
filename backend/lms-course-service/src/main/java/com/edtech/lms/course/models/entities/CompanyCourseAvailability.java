package com.edtech.lms.course.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CompanyCourseAvailability - Junction table for company-course relationships
 * 
 * Purpose: Manage which courses are available to which companies
 * - Org-level creators create courses (org_id only)
 * - Super Admin or Company Admin enables courses for specific companies
 * - Multiple companies can use the same course
 * - Company-level filtering prevents cross-tenant access
 * 
 * Example:
 * Course: Java Basics (creator: Bob, org_id=1) - org-level
 * Availability:
 *   - Engineering (company_id=1) - available=TRUE ✓
 *   - Sales (company_id=2) - available=TRUE ✓
 * Result: Both companies can assign this course to their employees
 */
@Entity
@Table(name = "svc_course_company_course_availability", indexes = {
    @Index(name = "idx_availability_company", columnList = "company_id"),
    @Index(name = "idx_availability_course", columnList = "course_id"),
    @Index(name = "idx_availability_company_course", columnList = "company_id,course_id,is_available")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCourseAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long availabilityId;

    /**
     * Company that can access this course
     */
    @Column(nullable = false)
    private String companyId;

    /**
     * The org-level course created by a creator
     */
    @Column(nullable = false)
    private Long courseId;

    /**
     * Whether this course is available to this company
     * TRUE = Company can assign to employees
     * FALSE = Course hidden from company (but record preserved for audit)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * Which user enabled this course for the company
     * Usually: SUPER_ADMIN or COMPANY_ADMIN
     */
    @Column(nullable = false)
    private String addedByUserId;

    /**
     * When this availability was created
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When this availability was last modified
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
