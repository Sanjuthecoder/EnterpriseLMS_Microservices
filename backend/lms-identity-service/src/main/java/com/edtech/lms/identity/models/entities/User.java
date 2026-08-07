package com.edtech.lms.identity.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.models.enums.UserStatus;
import java.time.LocalDateTime;

/**
 * User Entity - Multi-role users in the system
 * Supports: SUPER_ADMIN (platform level), COMPANY_ADMIN, EMPLOYEE, CREATOR
 * Row-level multi-tenancy: org_id, company_id (nullable for non-company users)
 */
@Entity
@Table(name = "svc_identity_users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_org_id", columnList = "org_id"),
    @Index(name = "idx_user_company_id", columnList = "company_id"),
    @Index(name = "idx_user_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = true)
    private String phone;

    /**
     * User's role in the system
     * SUPER_ADMIN: Platform administrator
     * COMPANY_ADMIN: Organization training admin
     * EMPLOYEE: Learner
     * CREATOR: Content creator
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = true)
    private Long orgId;

    /**
     * Nullable because SUPER_ADMIN doesn't belong to a company
     */
    /**
     * Nullable for SUPER_ADMIN and CREATOR (org-level users)
     * Required for COMPANY_ADMIN and EMPLOYEE (company-level users)
     */
    @Column(nullable = true)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(nullable = true)
    private String department;

    /**
     * Manager ID for employees (references another user)
     */
    @Column(nullable = true)
    private Long managerId;

    /**
     * Whether employee has completed profile setup
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean profileComplete = false;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = true)
    private String avatarUrl;

    @Column(nullable = true)
    private LocalDateTime lastLogin;

    @Column(nullable = true)
    private String resetToken;

    @Column(nullable = true)
    private LocalDateTime resetTokenExpiry;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        validateCompanyIdByRole();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateCompanyIdByRole();
    }

    /**
     * Validation: SUPER_ADMIN and CREATOR must NOT have a company_id
     * They operate at org level, independent of any company
     */
    private void validateCompanyIdByRole() {
        if (this.role == UserRole.SUPER_ADMIN) {
            this.companyId = null;  // Super Admin has no company
            this.orgId = null;      // Super Admin has no org (global)
        } else if (this.role == UserRole.CREATOR) {
            this.companyId = null;  // Creator has no company
            this.orgId = null;      // Creator has no organization (independent entity)
        } else {
            // COMPANY_ADMIN and EMPLOYEE must belong to both an organization and a company
            if (this.orgId == null || this.companyId == null) {
                throw new IllegalStateException("Company Admin and Employee must have both orgId and companyId");
            }
        }
    }
}
