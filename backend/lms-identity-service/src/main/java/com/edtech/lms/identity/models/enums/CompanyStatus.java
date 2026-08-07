package com.edtech.lms.identity.models.enums;

/**
 * Status of companies (training clients within organizations).
 */
public enum CompanyStatus {
    PENDING("Awaiting organization approval"),
    APPROVED("Approved by organization"),
    ACTIVE("Currently active"),
    SUSPENDED("Temporarily suspended"),
    INACTIVE("No longer active");

    private final String description;

    CompanyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
