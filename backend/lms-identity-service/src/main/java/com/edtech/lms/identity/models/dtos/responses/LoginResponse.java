package com.edtech.lms.identity.models.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String role;
    private Long orgId;
    private Long companyId;
    /** FREE or PREMIUM — drives frontend feature-flagging without additional API calls */
    private String subscriptionTier;

    public LoginResponse(String token, Long userId, String username, String email,
                         String role, Long orgId, Long companyId, String subscriptionTier) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.orgId = orgId;
        this.companyId = companyId;
        this.subscriptionTier = subscriptionTier;
    }
}

