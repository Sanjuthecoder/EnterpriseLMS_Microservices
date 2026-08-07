package com.edtech.lms.identity.services.admin;

import com.edtech.lms.identity.models.dtos.requests.OrganizationRequest;
import com.edtech.lms.identity.models.dtos.requests.ProvisionAdminRequest;
import com.edtech.lms.identity.models.entities.Company;
import com.edtech.lms.identity.models.entities.Organization;
import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.models.enums.OrganizationStatus;
import com.edtech.lms.identity.models.enums.UserRole;
import com.edtech.lms.identity.models.enums.UserStatus;
import com.edtech.lms.identity.repositories.CompanyRepository;
import com.edtech.lms.identity.repositories.OrganizationRepository;
import com.edtech.lms.identity.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SuperAdminService.
 *
 * Covers:
 * - Organization registration (happy path + duplicate detection)
 * - Organization approval (happy path + invalid state)
 * - Organization deactivation
 * - Company Admin provisioning
 * - Creator listing and approval
 * - Platform metrics aggregation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminService Unit Tests")
class SuperAdminServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private SuperAdminService superAdminService;

    private Organization activeOrg;
    private Organization pendingOrg;

    @BeforeEach
    void setUp() {
        activeOrg = new Organization();
        activeOrg.setOrgId(1L);
        activeOrg.setName("TechCorp");
        activeOrg.setEmail("tech@corp.com");
        activeOrg.setStatus(OrganizationStatus.ACTIVE);
        activeOrg.setFreeEmployees(100);
        activeOrg.setFreeCourses(10);

        pendingOrg = new Organization();
        pendingOrg.setOrgId(2L);
        pendingOrg.setName("StartupX");
        pendingOrg.setEmail("hello@startupx.com");
        pendingOrg.setStatus(OrganizationStatus.PENDING);
        pendingOrg.setFreeEmployees(100);
        pendingOrg.setFreeCourses(10);
    }

    // =========================================================================
    // ORGANIZATION REGISTRATION
    // =========================================================================

    @Test
    @DisplayName("registerOrganization — should create and return org with PENDING status")
    void registerOrganization_happyPath() {
        OrganizationRequest req = new OrganizationRequest();
        req.setName("NewCorp");
        req.setEmail("new@corp.com");

        when(organizationRepository.findByEmail("new@corp.com")).thenReturn(Optional.empty());
        when(organizationRepository.findByName("NewCorp")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        Organization result = superAdminService.registerOrganization(req);

        assertThat(result.getName()).isEqualTo("NewCorp");
        assertThat(result.getStatus()).isEqualTo(OrganizationStatus.PENDING);
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("registerOrganization — should throw if email already exists")
    void registerOrganization_duplicateEmail() {
        OrganizationRequest req = new OrganizationRequest();
        req.setName("AnotherCorp");
        req.setEmail("tech@corp.com");

        when(organizationRepository.findByEmail("tech@corp.com")).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> superAdminService.registerOrganization(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    @DisplayName("registerOrganization — should throw if name already exists")
    void registerOrganization_duplicateName() {
        OrganizationRequest req = new OrganizationRequest();
        req.setName("TechCorp");
        req.setEmail("unique@email.com");

        when(organizationRepository.findByEmail("unique@email.com")).thenReturn(Optional.empty());
        when(organizationRepository.findByName("TechCorp")).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> superAdminService.registerOrganization(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name already exists");
    }

    // =========================================================================
    // ORGANIZATION APPROVAL
    // =========================================================================

    @Test
    @DisplayName("approveOrganization — should set status to ACTIVE")
    void approveOrganization_happyPath() {
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(pendingOrg));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        Organization result = superAdminService.approveOrganization(2L);

        assertThat(result.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("approveOrganization — should throw for non-PENDING org")
    void approveOrganization_alreadyActive() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> superAdminService.approveOrganization(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING");
    }

    @Test
    @DisplayName("approveOrganization — should throw if org not found")
    void approveOrganization_notFound() {
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> superAdminService.approveOrganization(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // ORGANIZATION DEACTIVATION
    // =========================================================================

    @Test
    @DisplayName("deactivateOrganization — should set status to INACTIVE")
    void deactivateOrganization_happyPath() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(activeOrg));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        superAdminService.deactivateOrganization(1L);

        assertThat(activeOrg.getStatus()).isEqualTo(OrganizationStatus.INACTIVE);
    }

    // =========================================================================
    // COMPANY ADMIN PROVISIONING
    // =========================================================================

    @Test
    @DisplayName("provisionCompanyAdmin — should create company and user in one transaction")
    void provisionCompanyAdmin_happyPath() {
        ProvisionAdminRequest req = new ProvisionAdminRequest();
        req.setUsername("admin1");
        req.setEmail("admin1@corp.com");
        req.setPassword("SecurePass@1");
        req.setCompanyName("TechCorp Division");

        Company savedCompany = new Company();
        savedCompany.setCompanyId(10L);
        savedCompany.setOrgId(1L);
        savedCompany.setName("TechCorp Division");
        savedCompany.setAdminEmail("admin1@corp.com");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(activeOrg));
        when(userRepository.findByEmail("admin1@corp.com")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);
        when(passwordEncoder.encode("SecurePass@1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(100L);
            return u;
        });

        User result = superAdminService.provisionCompanyAdmin(1L, req);

        assertThat(result.getRole()).isEqualTo(UserRole.COMPANY_ADMIN);
        assertThat(result.getCompanyId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(companyRepository).save(any(Company.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("provisionCompanyAdmin — should throw for inactive org")
    void provisionCompanyAdmin_inactiveOrg() {
        ProvisionAdminRequest req = new ProvisionAdminRequest();
        req.setEmail("test@test.com");

        when(organizationRepository.findById(2L)).thenReturn(Optional.of(pendingOrg));

        assertThatThrownBy(() -> superAdminService.provisionCompanyAdmin(2L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive organization");
    }

    // =========================================================================
    // CREATOR MANAGEMENT
    // =========================================================================

    @Test
    @DisplayName("approveCreator — should activate a PENDING creator")
    void approveCreator_happyPath() {
        User creator = new User();
        creator.setUserId(5L);
        creator.setRole(UserRole.CREATOR);
        creator.setStatus(UserStatus.PENDING);

        when(userRepository.findById(5L)).thenReturn(Optional.of(creator));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = superAdminService.approveCreator(5L);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("approveCreator — should throw if user is not a creator")
    void approveCreator_wrongRole() {
        User employee = new User();
        employee.setUserId(6L);
        employee.setRole(UserRole.EMPLOYEE);

        when(userRepository.findById(6L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> superAdminService.approveCreator(6L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a CREATOR");
    }

    // =========================================================================
    // PLATFORM METRICS
    // =========================================================================

    @Test
    @DisplayName("getPlatformMetrics — should return all platform-level counts")
    void getPlatformMetrics_returnsAllKeys() {
        when(organizationRepository.count()).thenReturn(5L);
        when(organizationRepository.findAll()).thenReturn(List.of(activeOrg, pendingOrg));
        when(companyRepository.count()).thenReturn(12L);
        when(userRepository.count()).thenReturn(200L);
        when(userRepository.findByRole(UserRole.EMPLOYEE)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.CREATOR)).thenReturn(List.of());

        Map<String, Object> metrics = superAdminService.getPlatformMetrics();

        assertThat(metrics).containsKeys(
                "totalOrganizations", "activeOrganizations", "totalCompanies",
                "totalUsers", "totalEmployees", "pendingCreators");
        assertThat(metrics.get("totalOrganizations")).isEqualTo(5L);
    }
}
