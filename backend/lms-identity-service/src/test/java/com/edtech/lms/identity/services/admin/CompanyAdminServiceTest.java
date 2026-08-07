package com.edtech.lms.identity.services.admin;

import com.edtech.lms.identity.models.entities.User;
import com.edtech.lms.identity.repositories.OrganizationRepository;
import com.edtech.lms.identity.repositories.UserRepository;
import com.edtech.lms.identity.services.notification.NotificationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationPublisher NotificationPublisher;

    @InjectMocks
    private CompanyAdminService companyAdminService;

    @Test
    void testBulkImportEmployees_EmptyPassword_ShouldGenerateRandomPassword() throws Exception {
        // Given
        Long orgId = 1L;
        Long companyId = 100L;
        Map<String, String> record = Map.of(
                "email", "test@example.com",
                "username", "testuser",
                "password", ""
        );
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        // When
        Map<String, Object> report = companyAdminService.bulkImportEmployees(orgId, companyId, List.of(record));

        // Then
        assertEquals(1, report.get("successfullyImported"));
        
        // Capture the password sent to NotificationPublisher
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(NotificationPublisher).sendOnboardingCredentials(eq("test@example.com"), eq("testuser"), passwordCaptor.capture(), eq("EMPLOYEE"));
        
        String generatedPassword = passwordCaptor.getValue();
        assertNotNull(generatedPassword);
        assertFalse(generatedPassword.isEmpty());
        assertTrue(generatedPassword.startsWith("Temp@"));
        assertTrue(generatedPassword.length() >= 10);
    }

    @Test
    void testBulkImportEmployees_NotificationPublisherThrowsException_ShouldReportFailedEmails() throws Exception {
        // Given
        Long orgId = 1L;
        Long companyId = 100L;
        Map<String, String> record = Map.of(
                "email", "invalid@format",
                "username", "badmailuser"
        );
        
        when(userRepository.findByEmail("invalid@format")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        
        doThrow(new RuntimeException("Could not parse mail"))
                .when(NotificationPublisher).sendOnboardingCredentials(eq("invalid@format"), eq("badmailuser"), anyString(), eq("EMPLOYEE"));

        // When
        Map<String, Object> report = companyAdminService.bulkImportEmployees(orgId, companyId, List.of(record));

        // Then
        assertEquals(1, report.get("successfullyImported"));
        assertEquals(1, report.get("emailsFailed"));
        
        List<Map<String, String>> failedEmailsList = (List<Map<String, String>>) report.get("failedEmails");
        assertEquals(1, failedEmailsList.size());
        assertEquals("badmailuser", failedEmailsList.get(0).get("username"));
        assertEquals("invalid@format", failedEmailsList.get(0).get("email"));
        assertEquals("Could not parse mail", failedEmailsList.get(0).get("reason"));
    }
}
