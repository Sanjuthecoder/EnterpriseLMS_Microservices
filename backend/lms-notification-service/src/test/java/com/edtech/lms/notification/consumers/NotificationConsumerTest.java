package com.edtech.lms.notification.consumers;

import com.edtech.lms.notification.dtos.NotificationEvent;
import com.edtech.lms.notification.services.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class NotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void testConsumeOnboardingEvent() throws Exception {
        String payload = "{\"eventType\":\"ONBOARDING\",\"toEmail\":\"emp@company.com\",\"metadata\":{\"username\":\"johndoe\",\"password\":\"pass123\",\"roleName\":\"EMPLOYEE\"}}";
        NotificationEvent event = new NotificationEvent(
            "ONBOARDING",
            "emp@company.com",
            Map.of("username", "johndoe", "password", "pass123", "roleName", "EMPLOYEE")
        );
        org.mockito.Mockito.when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verify(emailService).sendOnboardingCredentials("emp@company.com", "johndoe", "pass123", "EMPLOYEE");
    }

    @Test
    void testConsumePasswordResetEvent() throws Exception {
        String payload = "{\"eventType\":\"PASSWORD_RESET\",\"toEmail\":\"admin@company.com\",\"metadata\":{\"username\":\"admin\",\"verificationCode\":\"123456\"}}";
        NotificationEvent event = new NotificationEvent(
            "PASSWORD_RESET",
            "admin@company.com",
            Map.of("username", "admin", "verificationCode", "123456")
        );
        org.mockito.Mockito.when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verify(emailService).sendPasswordResetCode("admin@company.com", "admin", "123456");
    }

    @Test
    void testConsumeUnknownEvent() throws Exception {
        String payload = "{\"eventType\":\"UNKNOWN_EVENT\",\"toEmail\":\"test@company.com\",\"metadata\":{}}";
        NotificationEvent event = new NotificationEvent(
            "UNKNOWN_EVENT",
            "test@company.com",
            Map.of()
        );
        org.mockito.Mockito.when(objectMapper.readValue(payload, NotificationEvent.class)).thenReturn(event);

        notificationConsumer.consumeNotificationEvent(payload);

        verifyNoMoreInteractions(emailService);
    }
}
