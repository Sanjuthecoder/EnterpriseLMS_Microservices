package com.edtech.lms.identity.models.enums;

/**
 * User roles in the Enterprise LMS system.
 * - SUPER_ADMIN: Platform administrator (manages organizations)
 * - COMPANY_ADMIN: Organization training administrator (manages employees, courses, assignments)
 * - EMPLOYEE: Learner taking courses
 * - CREATOR: Content creator (creates courses)
 */
public enum UserRole {
    SUPER_ADMIN("Super Administrator - Platform level"),
    COMPANY_ADMIN("Company Administrator - Organization level"),
    EMPLOYEE("Employee - Learner"),
    CREATOR("Content Creator");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
