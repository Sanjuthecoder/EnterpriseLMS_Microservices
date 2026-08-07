package com.edtech.lms.identity.services.admin;

import com.edtech.lms.identity.models.dtos.requests.OrganizationRequest;
import com.edtech.lms.identity.models.dtos.requests.ProvisionAdminRequest;
import com.edtech.lms.identity.models.entities.Company;
import com.edtech.lms.identity.models.entities.Organization;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.CompanyStatus;
import com.edtech.lms.identity.models.enums.OrganizationStatus;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.models.enums.UserStatus;
import com.edtech.lms.identity.repositories.CompanyRepository;
import com.edtech.lms.identity.repositories.OrganizationRepository;
import com.edtech.lms.identity.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SuperAdminService — Handles all platform-level administration.
 *
 * Responsibilities:
 * - Organization lifecycle (register, approve, deactivate)
 * - Company Admin provisioning
 * - Creator management (list, approve)
 * - Platform metrics aggregation
 *
 * Security: All methods assume the caller is SUPER_ADMIN.
 * Role enforcement is handled at the controller/gateway layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.edtech.lms.identity.services.notification.NotificationPublisher NotificationPublisher;

    // =========================================================================
    // ORGANIZATION MANAGEMENT
    // =========================================================================

    /**
     * Registers a new organization. Status defaults to PENDING.
     * Super Admin must explicitly approve via approveOrganization().
     */
    @Transactional
    public Organization registerOrganization(OrganizationRequest request) {
        log.info("Registering new organization: {}", request.getName());

        if (organizationRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An organization with this email already exists: " + request.getEmail());
        }
        if (organizationRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("An organization with this name already exists: " + request.getName());
        }

        Organization org = Organization.builder()
                .name(request.getName())
                .email(request.getEmail())
                .status(OrganizationStatus.PENDING)
                .freeEmployees(request.getFreeEmployees() != null ? request.getFreeEmployees() : 100)
                .freeCourses(request.getFreeCourses() != null ? request.getFreeCourses() : 10)
                .build();

        Organization saved = organizationRepository.save(org);
        log.info("Organization registered successfully with id={}", saved.getOrgId());
        return saved;
    }

    /**
     * Approves a PENDING organization, making it ACTIVE.
     */
    @Transactional
    public Organization approveOrganization(Long orgId) {
        log.info("Approving organization id={}", orgId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        if (org.getStatus() != OrganizationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING organizations can be approved. Current status: " + org.getStatus());
        }

        org.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(org);
        
        // Automatically provision a company admin so they get an email with credentials
        try {
            ProvisionAdminRequest adminReq = new ProvisionAdminRequest();
            adminReq.setUsername("Admin");
            adminReq.setEmail(org.getEmail());
            adminReq.setCompanyName(org.getName());
            
            // Generate a secure temporary password
            String tempPassword = "Temp" + System.currentTimeMillis() + "!";
            adminReq.setPassword(tempPassword);
            
            provisionCompanyAdmin(orgId, adminReq);
        } catch (Exception e) {
            log.error("Failed to automatically provision admin for org {}: {}", orgId, e.getMessage());
            // Proceed anyway, the organization is approved
        }
        
        return savedOrg;
    }

    /**
     * Returns all organizations with pagination.
     */
    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);
    }

    /**
     * Deactivates an organization. Does not delete data.
     */
    @Transactional
    public void deactivateOrganization(Long orgId) {
        log.info("Deactivating organization id={}", orgId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);
    }

    // =========================================================================
    // COMPANY ADMIN PROVISIONING
    // =========================================================================

    /**
     * Provisions a Company Admin within an organization.
     * Creates both the Company record and the COMPANY_ADMIN User in one transaction.
     */
    @Transactional
    public User provisionCompanyAdmin(Long orgId, ProvisionAdminRequest request) {
        log.info("Provisioning company admin for orgId={}, company={}", orgId, request.getCompanyName());

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot provision admin for an inactive organization.");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists: " + request.getEmail());
        }

        // Create Company record
        Company company = Company.builder()
                .orgId(orgId)
                .name(request.getCompanyName())
                .adminEmail(request.getEmail())
                .status(CompanyStatus.ACTIVE)
                .build();
        Company savedCompany = companyRepository.save(company);

        // Create COMPANY_ADMIN user
        User admin = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.COMPANY_ADMIN)
                .orgId(orgId)
                .companyId(savedCompany.getCompanyId())
                .status(UserStatus.ACTIVE)
                .profileComplete(false)
                .build();

        User savedUser = userRepository.save(admin);
        
        try {
            NotificationPublisher.sendOnboardingCredentials(request.getEmail(), request.getUsername(), request.getPassword(), "COMPANY_ADMIN");
        } catch (Exception ex) {
            log.warn("Company admin provisioned, but email failed: {}", ex.getMessage());
        }
        
        log.info("Company admin provisioned: userId={}, companyId={}", savedUser.getUserId(), savedCompany.getCompanyId());
        return savedUser;
    }

    /**
     * Returns all companies in the platform.
     */
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    // =========================================================================
    // CREATOR MANAGEMENT
    // =========================================================================

    /**
     * Returns all users with CREATOR role.
     */
    public List<User> listCreators() {
        return userRepository.findByRole(UserRole.CREATOR);
    }

    /**
     * Approves a pending creator, activating their account.
     */
    @Transactional
    public User approveCreator(Long creatorId) {
        log.info("Approving creator id={}", creatorId);
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorId));

        if (creator.getRole() != UserRole.CREATOR) {
            throw new IllegalStateException("User is not a CREATOR. Role: " + creator.getRole());
        }

        creator.setStatus(UserStatus.ACTIVE);
        return userRepository.save(creator);
    }

    // =========================================================================
    // PLATFORM METRICS
    // =========================================================================

    /**
     * Aggregates platform-level metrics for the Super Admin dashboard.
     */
    public Map<String, Object> getPlatformMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrganizations", organizationRepository.count());
        metrics.put("activeOrganizations", organizationRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE).count());
        metrics.put("totalCompanies", companyRepository.count());
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalEmployees", userRepository.findByRole(UserRole.EMPLOYEE).size());
        metrics.put("pendingCreators", userRepository.findByRole(UserRole.CREATOR).stream()
                .filter(u -> u.getStatus() == UserStatus.PENDING).count());
        return metrics;
    }
}
