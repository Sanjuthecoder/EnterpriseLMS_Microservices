package com.edtech.lms.notification;

import com.edtech.lms.notification.dtos.NotificationEvent;
import com.edtech.lms.notification.consumers.NotificationConsumer;
import com.edtech.lms.notification.services.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceIntegrationTest {

    @Mock
    private EmailService emailService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void testOnboardingEventConsuming() throws Exception {
        String payload = "{\"eventType\":\"ONBOARDING\",\"toEmail\":\"test@example.com\",\"metadata\":{\"username\":\"testuser\",\"password\":\"temp123\",\"roleName\":\"EMPLOYEE\"}}";
        
        NotificationEvent event = new NotificationEvent();
        event.setEventType("ONBOARDING");
        event.setToEmail("test@example.com");
        event.setMetadata(Map.of("username", "testuser", "password", "temp123", "roleName", "EMPLOYEE"));
        
        when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verify(emailService, times(1)).sendOnboardingCredentials("test@example.com", "testuser", "temp123", "EMPLOYEE");
    }

    @Test
    void testPasswordResetEventConsuming() throws Exception {
        String payload = "{\"eventType\":\"PASSWORD_RESET\",\"toEmail\":\"reset@example.com\",\"metadata\":{\"username\":\"resetuser\",\"verificationCode\":\"123456\"}}";
        
        NotificationEvent event = new NotificationEvent();
        event.setEventType("PASSWORD_RESET");
        event.setToEmail("reset@example.com");
        event.setMetadata(Map.of("username", "resetuser", "verificationCode", "123456"));
        
        when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verify(emailService, times(1)).sendPasswordResetCode("reset@example.com", "resetuser", "123456");
    }

    @Test
    void testPremiumUpgradeEventConsuming() throws Exception {
        String payload = "{\"eventType\":\"PREMIUM_UPGRADE\",\"toEmail\":\"admin@company.com\",\"metadata\":{\"username\":\"adminuser\"}}";
        
        NotificationEvent event = new NotificationEvent();
        event.setEventType("PREMIUM_UPGRADE");
        event.setToEmail("admin@company.com");
        event.setMetadata(Map.of("username", "adminuser"));
        
        when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verify(emailService, times(1)).sendPremiumUpgradeConfirmation("admin@company.com", "adminuser");
    }
}
