package com.edtech.lms.identity.controllers;

import com.edtech.lms.identity.models.dtos.requests.OrganizationRequest;
import com.edtech.lms.identity.models.dtos.requests.ProvisionAdminRequest;
import com.edtech.lms.identity.models.entities.Organization;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.services.admin.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SuperAdminController — REST API for platform-level administration.
 *
 * Routes: /api/super-admin/**
 * Security: JWT validated at API Gateway; X-User-Role=SUPER_ADMIN header enforced.
 * Logging: All mutations are logged at INFO level for auditability.
 */
@Slf4j
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // =========================================================================
    // ORGANIZATION MANAGEMENT
    // =========================================================================

    /**
     * POST /api/super-admin/organizations
     * Registers a new tenant organization. Defaults to PENDING status.
     */
    @PostMapping("/organizations")
    public ResponseEntity<?> registerOrganization(@Valid @RequestBody OrganizationRequest request) {
        try {
            log.info("Super admin registering organization: {}", request.getName());
            Organization org = superAdminService.registerOrganization(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(org);
        } catch (IllegalArgumentException e) {
            log.warn("Organization registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/super-admin/organizations/{orgId}/approve
     * Approves a PENDING organization to ACTIVE.
     */
    @PatchMapping("/organizations/{orgId}/approve")
    public ResponseEntity<?> approveOrganization(@PathVariable Long orgId) {
        try {
            Organization org = superAdminService.approveOrganization(orgId);
            return ResponseEntity.ok(org);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Organization approval failed for id={}: {}", orgId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/super-admin/organizations
     * Returns paginated list of all organizations.
     */
    @GetMapping("/organizations")
    public ResponseEntity<Page<Organization>> getAllOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.getAllOrganizations(PageRequest.of(page, size)));
    }

    /**
     * DELETE /api/super-admin/organizations/{orgId}
     * Deactivates an organization. Soft delete — data is preserved.
     */
    @DeleteMapping("/organizations/{orgId}")
    public ResponseEntity<?> deactivateOrganization(@PathVariable Long orgId) {
        try {
            superAdminService.deactivateOrganization(orgId);
            return ResponseEntity.ok(Map.of("message", "Organization deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/super-admin/organizations/{orgId}/admins
     * Provisions a Company Admin under the given organization.
     * Creates both a Company and a User in one atomic transaction.
     */
    @PostMapping("/organizations/{orgId}/admins")
    public ResponseEntity<?> provisionCompanyAdmin(
            @PathVariable Long orgId,
            @Valid @RequestBody ProvisionAdminRequest request) {
        try {
            log.info("Provisioning company admin for orgId={}", orgId);
            User admin = superAdminService.provisionCompanyAdmin(orgId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(admin);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Admin provisioning failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/super-admin/companies
     * Returns all companies in the platform.
     */
    @GetMapping("/companies")
    public ResponseEntity<List<com.edtech.lms.identity.models.entities.Company>> getAllCompanies() {
        return ResponseEntity.ok(superAdminService.getAllCompanies());
    }

    // =========================================================================
    // CREATOR MANAGEMENT
    // =========================================================================

    /**
     * GET /api/super-admin/creators
     * Lists all users with CREATOR role.
     */
    @GetMapping("/creators")
    public ResponseEntity<List<User>> listCreators() {
        return ResponseEntity.ok(superAdminService.listCreators());
    }

    /**
     * PATCH /api/super-admin/creators/{creatorId}/approve
     * Activates a pending creator's account.
     */
    @PatchMapping("/creators/{creatorId}/approve")
    public ResponseEntity<?> approveCreator(@PathVariable Long creatorId) {
        try {
            User creator = superAdminService.approveCreator(creatorId);
            return ResponseEntity.ok(creator);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Creator approval failed for id={}: {}", creatorId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // DASHBOARD METRICS
    // =========================================================================

    /**
     * GET /api/super-admin/dashboard/metrics
     * Returns aggregated platform-level metrics for the overview dashboard.
     */
    @GetMapping("/dashboard/metrics")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        return ResponseEntity.ok(superAdminService.getPlatformMetrics());
    }
}
