package com.edtech.lms.identity.services.auth;

import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.repositories.UserRepository;
import com.edtech.lms.identity.security.JwtTokenProvider;
import com.edtech.lms.identity.models.dtos.requests.LoginRequest;
import com.edtech.lms.identity.models.dtos.responses.LoginResponse;
import com.edtech.lms.identity.models.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private com.edtech.lms.identity.repositories.OrganizationRepository organizationRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLogin_Employee_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("employee@lms.com");
        req.setPassword("password123");

        User mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setUsername("employee");
        mockUser.setEmail("employee@lms.com");
        mockUser.setPasswordHash("hashed_password123");
        mockUser.setRole(UserRole.EMPLOYEE);
        mockUser.setOrgId(100L);
        mockUser.setCompanyId(200L);
        mockUser.setStatus(com.edtech.lms.identity.models.enums.UserStatus.ACTIVE);

        com.edtech.lms.identity.models.entities.Organization org = new com.edtech.lms.identity.models.entities.Organization();
        org.setStatus(com.edtech.lms.identity.models.enums.OrganizationStatus.ACTIVE);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashed_password123")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(org));
        when(tokenProvider.generateToken(1L, "employee", 100L, 200L, UserRole.EMPLOYEE, "FREE")).thenReturn("employee_jwt");

        LoginResponse res = authenticationService.authenticateUser(req);
        assertEquals("employee_jwt", res.getToken());
        assertEquals("EMPLOYEE", res.getRole());
    }

    @Test
    public void testLogin_CompanyAdmin_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@company.com");
        req.setPassword("pass");

        User mockUser = new User();
        mockUser.setUserId(2L);
        mockUser.setUsername("company_admin");
        mockUser.setPasswordHash("hash");
        mockUser.setRole(UserRole.COMPANY_ADMIN);
        mockUser.setOrgId(101L);
        mockUser.setCompanyId(201L);
        mockUser.setStatus(com.edtech.lms.identity.models.enums.UserStatus.ACTIVE);

        com.edtech.lms.identity.models.entities.Organization org = new com.edtech.lms.identity.models.entities.Organization();
        org.setStatus(com.edtech.lms.identity.models.enums.OrganizationStatus.ACTIVE);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(organizationRepository.findById(101L)).thenReturn(Optional.of(org));
        when(tokenProvider.generateToken(2L, "company_admin", 101L, 201L, UserRole.COMPANY_ADMIN, "FREE")).thenReturn("admin_jwt");

        LoginResponse res = authenticationService.authenticateUser(req);
        assertEquals("admin_jwt", res.getToken());
        assertEquals("COMPANY_ADMIN", res.getRole());
    }

    @Test
    public void testLogin_SuperAdmin_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("super@lms.com");
        req.setPassword("superpass");

        User mockUser = new User();
        mockUser.setUserId(3L);
        mockUser.setUsername("super_admin");
        mockUser.setPasswordHash("hash");
        mockUser.setRole(UserRole.SUPER_ADMIN);
        mockUser.setOrgId(null);
        mockUser.setCompanyId(null);
        mockUser.setStatus(com.edtech.lms.identity.models.enums.UserStatus.ACTIVE);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProvider.generateToken(3L, "super_admin", null, null, UserRole.SUPER_ADMIN, "FREE")).thenReturn("super_jwt");

        LoginResponse res = authenticationService.authenticateUser(req);
        assertEquals("super_jwt", res.getToken());
        assertEquals("SUPER_ADMIN", res.getRole());
    }

    @Test
    public void testLogin_Creator_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("creator@lms.com");
        req.setPassword("creatorpass");

        User mockUser = new User();
        mockUser.setUserId(4L);
        mockUser.setUsername("creator");
        mockUser.setPasswordHash("hash");
        mockUser.setRole(UserRole.CREATOR);
        mockUser.setOrgId(null);
        mockUser.setCompanyId(null);
        mockUser.setStatus(com.edtech.lms.identity.models.enums.UserStatus.ACTIVE);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProvider.generateToken(4L, "creator", null, null, UserRole.CREATOR, "FREE")).thenReturn("creator_jwt");

        LoginResponse res = authenticationService.authenticateUser(req);
        assertEquals("creator_jwt", res.getToken());
        assertEquals("CREATOR", res.getRole());
    }
}
