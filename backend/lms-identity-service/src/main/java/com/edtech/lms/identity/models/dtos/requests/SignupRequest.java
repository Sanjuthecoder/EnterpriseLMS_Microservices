package com.edtech.lms.identity.models.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String username;
    private String email;
    private String password;
    private String phone;
    private Long orgId;
    private Long companyId; // Null for Creator/Super Admin

    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public Long getOrgId() { return orgId; }
    public Long getCompanyId() { return companyId; }
}
