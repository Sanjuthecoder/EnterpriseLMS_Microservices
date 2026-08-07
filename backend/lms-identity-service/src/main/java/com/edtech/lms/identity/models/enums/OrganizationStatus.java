package com.edtech.lms.identity.models.enums;

/**
 * Status of organizations (SaaS tenants).
 */
public enum OrganizationStatus {
    PENDING("Awaiting super admin approval"),
    APPROVED("Approved by super admin"),
    ACTIVE("Currently active"),
    SUSPENDED("Temporarily suspended"),
    INACTIVE("No longer active");

    private final String description;

    OrganizationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
