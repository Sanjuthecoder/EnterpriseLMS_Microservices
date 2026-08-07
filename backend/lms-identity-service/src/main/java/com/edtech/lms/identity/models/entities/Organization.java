package com.edtech.lms.identity.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.identity.models.enums.OrganizationStatus;
import java.time.LocalDateTime;

/**
 * Organization Entity - Represents SaaS tenants
 * Each organization can have multiple companies
 * Row-level multi-tenancy: org_id is the partition key
 */
@Entity
@Table(name = "svc_identity_organizations", indexes = {
    @Index(name = "idx_org_status", columnList = "status"),
    @Index(name = "idx_org_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orgId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer freeEmployees = 100;

    @Column(nullable = false)
    @Builder.Default
    private Integer freeCourses = 10;

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
