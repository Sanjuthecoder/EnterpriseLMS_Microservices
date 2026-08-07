package com.edtech.lms.identity.controllers.auth;

import com.edtech.lms.identity.models.dtos.requests.LoginRequest;
import com.edtech.lms.identity.models.dtos.requests.SignupRequest;
import com.edtech.lms.identity.models.dtos.responses.LoginResponse;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.services.auth.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.edtech.lms.identity.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @GetMapping("/organizations")
    public ResponseEntity<?> getActiveOrganizations() {
        return ResponseEntity.ok(authenticationService.getActiveOrganizations());
    }

    @GetMapping("/organizations/{orgId}/companies")
    public ResponseEntity<?> getCompaniesByOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(authenticationService.getCompaniesByOrg(orgId));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authenticationService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/employee/signup")
    public ResponseEntity<?> registerEmployee(@RequestBody SignupRequest signupRequest) {
        User user = authenticationService.registerUser(signupRequest, UserRole.EMPLOYEE);
        return ResponseEntity.ok("Employee registered successfully! Pending admin approval.");
    }

    @PostMapping("/company-admin/signup")
    public ResponseEntity<?> registerCompanyAdmin(@RequestBody SignupRequest signupRequest) {
        User user = authenticationService.registerUser(signupRequest, UserRole.COMPANY_ADMIN);
        return ResponseEntity.ok("Company Admin registered successfully! Pending super admin approval.");
    }

    @PostMapping("/super-admin/signup")
    public ResponseEntity<?> registerSuperAdmin(@RequestBody SignupRequest signupRequest) {
        // Note: In a real system, super admin registration should be highly restricted
        User user = authenticationService.registerUser(signupRequest, UserRole.SUPER_ADMIN);
        return ResponseEntity.ok("Super Admin registered successfully!");
    }
    
    @PostMapping("/creator/signup")
    public ResponseEntity<?> registerCreator(@RequestBody SignupRequest signupRequest) {
        User user = authenticationService.registerUser(signupRequest, UserRole.CREATOR);
        return ResponseEntity.ok("Creator registered successfully!");
    }

    @GetMapping("/companies/{companyId}/theme")
    public ResponseEntity<?> getCompanyTheme(@PathVariable Long companyId) {
        return ResponseEntity.ok(authenticationService.getCompanyTheme(companyId));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody com.edtech.lms.identity.models.dtos.requests.ForgotPasswordRequest request) {
        authenticationService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok("Verification code sent to email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody com.edtech.lms.identity.models.dtos.requests.ResetPasswordRequest request) {
        authenticationService.verifyAndResetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully.");
    }
}
