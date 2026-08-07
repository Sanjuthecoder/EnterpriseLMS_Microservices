package com.edtech.lms.identity.services.auth;

import com.edtech.lms.identity.models.dtos.requests.LoginRequest;
import com.edtech.lms.identity.models.dtos.requests.SignupRequest;
import com.edtech.lms.identity.models.dtos.responses.LoginResponse;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.models.enums.UserStatus;
import com.edtech.lms.identity.repositories.UserRepository;
import com.edtech.lms.identity.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import com.edtech.lms.identity.exceptions.*;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final com.edtech.lms.identity.repositories.OrganizationRepository organizationRepository;
    private final com.edtech.lms.identity.repositories.CompanyRepository companyRepository;
    private final com.edtech.lms.identity.services.notification.NotificationPublisher NotificationPublisher;
    public User registerUser(SignupRequest signupRequest, UserRole role) {
        // Check if username or email exists
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Error: Email is already in use!");
        }
        if (userRepository.findByUsername(signupRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Error: Username is already taken!");
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .passwordHash(passwordEncoder.encode(signupRequest.getPassword()))
                .phone(signupRequest.getPhone())
                .orgId(signupRequest.getOrgId())
                .companyId(signupRequest.getCompanyId())
                .role(role)
                .status(UserStatus.PENDING) // Needs approval
                .build();
                
        // Validation for company_id will be handled by @PrePersist in User.java
        return userRepository.save(user);
    }

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Error: Invalid email or password."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Error: Invalid email or password.");
        }

        validateUserStatusAndOrganization(user);
        
        String subscriptionTier = resolveSubscriptionTier(user);
        
        final String jwt = tokenProvider.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getOrgId(),
                user.getCompanyId(),
                user.getRole(),
                subscriptionTier
        );

        return new LoginResponse(
                jwt,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString(),
                user.getOrgId(),
                user.getCompanyId(),
                subscriptionTier
        );
    }

    private void validateUserStatusAndOrganization(User user) {
        if (user.getStatus() != UserStatus.ACTIVE && user.getRole() != UserRole.SUPER_ADMIN) {
            throw new InactiveAccountException("Error: Account is not active.");
        }

        if (user.getOrgId() != null) {
            com.edtech.lms.identity.models.entities.Organization org = organizationRepository.findById(user.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Organization not found."));
            if (org.getStatus() != com.edtech.lms.identity.models.enums.OrganizationStatus.ACTIVE) {
                throw new InactiveAccountException("Error: Organization is inactive.");
            }
        }
    }

    private String resolveSubscriptionTier(User user) {
        if (user.getRole() == UserRole.COMPANY_ADMIN || user.getRole() == UserRole.EMPLOYEE) {
            Long lookupId = user.getCompanyId() != null ? user.getCompanyId() : user.getOrgId();
            if (lookupId != null) {
                return companyRepository.findById(lookupId)
                        .map(c -> c.getSubscriptionTier() != null ? c.getSubscriptionTier() : "FREE")
                        .orElse("FREE");
            }
        }
        return "FREE";
    }

    public java.util.List<java.util.Map<String, Object>> getActiveOrganizations() {
        return organizationRepository.findAll().stream()
                .filter(org -> org.getStatus() == com.edtech.lms.identity.models.enums.OrganizationStatus.ACTIVE)
                .map(org -> java.util.Map.of("orgId", (Object)org.getOrgId(), "name", (Object)org.getName()))
                .toList();
    }

    public java.util.List<java.util.Map<String, Object>> getCompaniesByOrg(Long orgId) {
        return companyRepository.findByOrgId(orgId).stream()
                .filter(comp -> comp.getStatus() == com.edtech.lms.identity.models.enums.CompanyStatus.ACTIVE)
                .map(comp -> java.util.Map.of("companyId", (Object)comp.getCompanyId(), "name", (Object)comp.getName()))
                .toList();
    }

    public java.util.Map<String, Object> getCompanyTheme(Long companyId) {
        com.edtech.lms.identity.models.entities.Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return java.util.Map.of(
                "logoUrl", company.getLogoUrl() != null ? company.getLogoUrl() : "",
                "companyName", company.getName() != null ? company.getName() : "",
                "themeConfig", company.getThemeConfig() != null ? company.getThemeConfig() : java.util.Map.of()
        );
    }



    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Error: User with this email not found."));

        // Generate 6 digit code
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        
        user.setResetToken(code);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        NotificationPublisher.sendPasswordResetCode(user.getEmail(), user.getUsername(), code);
    }

    public void verifyAndResetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Error: User with this email not found."));

        if (user.getResetToken() == null || !user.getResetToken().equals(token)) {
            throw new InvalidCredentialsException("Error: Invalid verification code.");
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new InvalidCredentialsException("Error: Verification code has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}
