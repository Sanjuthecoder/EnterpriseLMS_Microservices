package com.edtech.lms.identity.mappers;

import com.edtech.lms.identity.models.dtos.responses.CompanyResponse;
import com.edtech.lms.identity.models.dtos.responses.UserResponse;
import com.edtech.lms.identity.models.entities.Company;
import com.edtech.lms.identity.models.entities.User;

public class IdentityMapper {

    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .orgId(user.getOrgId())
                .companyId(user.getCompanyId())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .department(user.getDepartment())
                .profileComplete(user.getProfileComplete() != null ? user.getProfileComplete() : false)
                .build();
    }

    public static CompanyResponse toCompanyResponse(Company company) {
        if (company == null) return null;
        return CompanyResponse.builder()
                .companyId(company.getCompanyId())
                .name(company.getName())
                .orgId(company.getOrgId())
                .logoUrl(company.getLogoUrl())
                .themeConfig(company.getThemeConfig())
                .status(company.getStatus() != null ? company.getStatus().name() : null)
                .build();
    }
}
