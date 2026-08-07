package com.edtech.lms.identity.controllers;

import com.edtech.lms.identity.models.dtos.responses.CompanyResponse;
import com.edtech.lms.identity.models.dtos.responses.UserResponse;
import com.edtech.lms.identity.mappers.IdentityMapper;
import com.edtech.lms.identity.models.entities.Company;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.services.admin.CompanyAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CompanyAdminController — REST API for company-scoped administration.
 *
 * Routes: /api/company-admin/**
 * Multi-tenancy: companyId is extracted from the JWT via X-Company-Id header injected by Gateway.
 * Security: Does not trust client-provided companyId — uses gateway-injected header only.
 */
@Slf4j
@RestController
@RequestMapping("/api/company-admin")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final CompanyAdminService companyAdminService;

    // =========================================================================
    // COMPANY INFO & THEME
    // =========================================================================

    /**
     * GET /api/company-admin/company
     * Returns the company profile for the logged-in company admin.
     */
    @GetMapping("/company")
    public ResponseEntity<CompanyResponse> getCompany(@RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        Company company = companyAdminService.getCompany(Long.parseLong(companyIdHeader));
        return ResponseEntity.ok(IdentityMapper.toCompanyResponse(company));
    }

    /**
     * PUT /api/company-admin/theme
     * Updates the company's branding/theme configuration.
     * Accepts a JSON body with themeConfig and optional logoUrl.
     */
    @PutMapping("/theme")
    public ResponseEntity<CompanyResponse> updateTheme(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader,
            @RequestBody Map<String, Object> body) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        Long companyId = Long.parseLong(companyIdHeader);
        String logoUrl = body.containsKey("logoUrl") ? (String) body.get("logoUrl") : null;

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode themeConfig = mapper.valueToTree(body.get("themeConfig") != null ? body.get("themeConfig") : body);

        Company updated = companyAdminService.updateTheme(companyId, themeConfig, logoUrl);
        return ResponseEntity.ok(IdentityMapper.toCompanyResponse(updated));
    }

    // =========================================================================
    // EMPLOYEE MANAGEMENT
    // =========================================================================

    /**
     * GET /api/company-admin/employees
     * Returns all employees for the company (non-paged).
     */
    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getEmployees(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        List<UserResponse> employees = companyAdminService.getCompanyEmployees(Long.parseLong(companyIdHeader))
                .stream()
                .map(IdentityMapper::toUserResponse)
                .toList();
        return ResponseEntity.ok(employees);
    }

    /**
     * GET /api/company-admin/employees/paged
     * Returns paginated employees for the company.
     */
    @GetMapping("/employees/paged")
    public ResponseEntity<Page<UserResponse>> getEmployeesPaged(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        Page<UserResponse> employees = companyAdminService.getCompanyEmployeesPaged(
                Long.parseLong(companyIdHeader), PageRequest.of(page, size))
                .map(IdentityMapper::toUserResponse);
        return ResponseEntity.ok(employees);
    }

    /**
     * POST /api/company-admin/employees/{employeeId}/approve
     * Approves a pending employee. Must belong to the same company.
     */
    @PostMapping("/employees/{employeeId}/approve")
    public ResponseEntity<UserResponse> approveEmployee(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader,
            @PathVariable Long employeeId) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        User approved = companyAdminService.approveEmployee(Long.parseLong(companyIdHeader), employeeId);
        return ResponseEntity.ok(IdentityMapper.toUserResponse(approved));
    }

    /**
     * POST /api/company-admin/employees/bulk-import
     * Imports a list of employees. Expects an array of {username, email, department, password}.
     * Returns a summary report with successes and failures.
     */
    @PostMapping("/employees/bulk-import")
    public ResponseEntity<?> bulkImportEmployees(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestBody Map<String, Object> body) {
        if (companyIdHeader == null || companyIdHeader.isBlank()) {
            throw new SecurityException("Missing company context");
        }
        Long companyId = Long.parseLong(companyIdHeader);
        Long orgId = orgIdHeader != null && !orgIdHeader.isBlank() ? Long.parseLong(orgIdHeader) : 0L;

        @SuppressWarnings("unchecked")
        List<Map<String, String>> employees = (List<Map<String, String>>) body.get("employees");
        if (employees == null || employees.isEmpty()) {
            throw new IllegalArgumentException("No employee records provided");
        }

        Map<String, Object> report = companyAdminService.bulkImportEmployees(orgId, companyId, employees);
        int failed = (int) report.get("failedImports");
        return failed > 0 ? ResponseEntity.status(207).body(report) : ResponseEntity.ok(report);
    }

    /**
     * GET /api/company-admin/analytics/roi
     * Returns basic training analytics (completion rate, hours).
     */
    @GetMapping("/analytics/roi")
    public ResponseEntity<?> getAnalytics(
            @RequestHeader(value = "X-Company-Id", required = false) String companyIdHeader) {
        // Basic stub — real implementation would call course-service or aggregate from enrollments
        Map<String, Object> analytics = Map.of(
                "averageCompletionRate", 75.0,
                "totalTrainingHours", 120,
                "averageUplift", 15.2
        );
        return ResponseEntity.ok(analytics);
    }
}
