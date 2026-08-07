package com.edtech.lms.identity.models.entities;

import jakarta.persistence.*;
import lombok.*;
import com.edtech.lms.identity.models.enums.CompanyStatus;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.SqlTypes;

/**
 * Company Entity - Represents training clients within an organization
 * Each company belongs to one organization
 * Row-level multi-tenancy: org_id + company_id together form the partition
 */
@Entity
@Table(name = "svc_identity_companies", indexes = {
    @Index(name = "idx_company_org_id", columnList = "org_id"),
    @Index(name = "idx_company_status", columnList = "status"),
    @Index(name = "idx_company_email", columnList = "admin_email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String adminEmail;

    @Column(columnDefinition = "LONGTEXT")
    private String logoUrl;

    /**
     * theme_config stored as JSON
     * Example: { "primaryColor": "#007bff", "secondaryColor": "#6c757d", "fontFamily": "Arial", "customCSS": "..." }
     */
    @Column(columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    private JsonNode themeConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.PENDING;

    /**
     * Subscription tier for this company.
     * Values: FREE (default) | PREMIUM
     * Set to PREMIUM by CompanySubscriptionConsumer when a payment.success
     * Kafka event is received from lms-payment-service.
     */
    @Column(nullable = false)
    @Builder.Default
    private String subscriptionTier = "FREE";

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
