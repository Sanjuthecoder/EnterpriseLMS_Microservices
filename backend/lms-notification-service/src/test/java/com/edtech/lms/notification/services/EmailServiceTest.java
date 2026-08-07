package com.edtech.lms.notification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "onboardingTemplate", "Hello {{username}}, Role: {{roleName}}, Password: {{password}}");
        ReflectionTestUtils.setField(emailService, "passwordResetTemplate", "Hi {{username}}, Code: {{verificationCode}}");
    }

    @Test
    void testSendOnboardingCredentials() {
        emailService.sendOnboardingCredentials("emp@company.com", "johndoe", "TempPass123", "EMPLOYEE");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("emp@company.com", sentMessage.getTo()[0]);
        assertEquals("Welcome to Enterprise LMS - Your EMPLOYEE Credentials", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("johndoe"));
        assertTrue(sentMessage.getText().contains("EMPLOYEE"));
        assertTrue(sentMessage.getText().contains("TempPass123"));
    }

    @Test
    void testSendPasswordResetCode() {
        emailService.sendPasswordResetCode("admin@company.com", "adminuser", "987654");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("admin@company.com", sentMessage.getTo()[0]);
        assertEquals("Enterprise LMS - Password Reset Verification Code", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("adminuser"));
        assertTrue(sentMessage.getText().contains("987654"));
    }
}
