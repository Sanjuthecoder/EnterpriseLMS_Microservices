package com.edtech.lms.identity.models.enums;

/**
 * Status of users in the system.
 */
public enum UserStatus {
    PENDING("Awaiting approval"),
    ACTIVE("Active user"),
    SUSPENDED("Temporarily suspended"),
    INACTIVE("Deactivated");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
