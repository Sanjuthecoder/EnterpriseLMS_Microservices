package com.edtech.lms.identity.models.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private Long orgId;
    private Long companyId;
    private String role;
    private String status;
    private String department;
    private boolean profileComplete;
}
