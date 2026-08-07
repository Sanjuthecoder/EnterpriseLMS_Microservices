package com.edtech.lms.identity.services.admin;

import com.edtech.lms.identity.models.entities.Company;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.CompanyStatus;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.models.enums.UserStatus;
import com.edtech.lms.identity.repositories.CompanyRepository;
import com.edtech.lms.identity.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CompanyAdminService — Handles company-scoped administration.
 *
 * Responsibilities:
 * - Employee roster management (list, approve, bulk import)
 * - Company theme/branding updates
 * - Profile retrieval for company admin's own company
 *
 * Multi-tenancy: All operations are scoped to the caller's companyId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAdminService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final com.edtech.lms.identity.services.notification.NotificationPublisher NotificationPublisher;

    // =========================================================================
    // COMPANY INFO
    // =========================================================================

    public Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
    }

    @Transactional
    public Company updateTheme(Long companyId, JsonNode themeConfig, String logoUrl) {
        log.info("Updating theme for companyId={}", companyId);
        Company company = getCompany(companyId);
        company.setThemeConfig(themeConfig);
        if (logoUrl != null) {
            company.setLogoUrl(logoUrl);
        }
        return companyRepository.save(company);
    }

    // =========================================================================
    // EMPLOYEE MANAGEMENT
    // =========================================================================

    public List<User> getCompanyEmployees(Long companyId) {
        return userRepository.findByCompanyIdAndRole(companyId, UserRole.EMPLOYEE);
    }

    public Page<User> getCompanyEmployeesPaged(Long companyId, Pageable pageable) {
        return userRepository.findByCompanyIdAndRole(companyId, UserRole.EMPLOYEE, pageable);
    }

    @Transactional
    public User approveEmployee(Long companyId, Long employeeId) {
        log.info("Approving employee id={} for companyId={}", employeeId, companyId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        if (!companyId.equals(employee.getCompanyId())) {
            throw new SecurityException("Access denied: employee does not belong to your company.");
        }
        if (employee.getRole() != UserRole.EMPLOYEE) {
            throw new IllegalStateException("User is not an EMPLOYEE. Role: " + employee.getRole());
        }

        employee.setStatus(UserStatus.ACTIVE);
        return userRepository.save(employee);
    }

    /**
     * Bulk import employees from a list of pre-parsed records.
     * Returns a summary with successful imports and failures.
     */
    @Transactional
    public java.util.Map<String, Object> bulkImportEmployees(Long orgId, Long companyId, List<java.util.Map<String, String>> records) {
        log.info("Bulk importing {} employees for companyId={}", records.size(), companyId);
        int success = 0;
        int failed = 0;
        int emailsFailed = 0;
        List<String> errors = new java.util.ArrayList<>();
        List<java.util.Map<String, String>> failedEmailsList = new java.util.ArrayList<>();

        for (java.util.Map<String, String> record : records) {
            String email = record.get("email");
            String username = record.get("username");
            try {
                String password = record.get("password");
                if (password == null || password.trim().isEmpty()) {
                    password = "Temp@" + java.util.UUID.randomUUID().toString().substring(0, 8);
                }

                if (email == null || email.trim().isEmpty() || username == null || username.trim().isEmpty()) {
                    errors.add(String.format("Row missing email or username (Provided Name: '%s', Email: '%s')", 
                        username != null ? username : "N/A", 
                        email != null ? email : "N/A"));
                    failed++;
                    continue;
                }
                if (userRepository.findByEmail(email).isPresent()) {
                    errors.add(String.format("Row for %s: Email already exists (%s)", username, email));
                    failed++;
                    continue;
                }

                User employee = User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(UserRole.EMPLOYEE)
                        .orgId(orgId)
                        .companyId(companyId)
                        .status(UserStatus.ACTIVE)
                        .department(record.get("department"))
                        .profileComplete(false)
                        .build();

                userRepository.save(employee);
                
                try {
                    NotificationPublisher.sendOnboardingCredentials(email, username, password, "EMPLOYEE");
                } catch (Exception ex) {
                    log.warn("Employee {} imported, but email failed: {}", username, ex.getMessage());
                    emailsFailed++;
                    failedEmailsList.add(java.util.Map.of(
                            "username", username,
                            "email", email,
                            "reason", ex.getMessage() != null ? ex.getMessage() : "Unknown Mail Error"
                    ));
                }
                
                success++;
            } catch (Exception e) {
                log.warn("Failed to import employee record: {}", e.getMessage());
                errors.add(String.format("Row for %s (%s): Import error - %s", 
                    username != null ? username : "N/A", 
                    email != null ? email : "N/A", 
                    e.getMessage()));
                failed++;
            }
        }

        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("successfullyImported", success);
        report.put("failedImports", failed);
        report.put("errors", errors);
        report.put("emailsFailed", emailsFailed);
        report.put("failedEmails", failedEmailsList);
        return report;
    }
}
