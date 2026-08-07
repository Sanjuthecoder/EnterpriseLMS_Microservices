package com.edtech.lms.notification.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private String eventType; // "ONBOARDING" or "PASSWORD_RESET"
    private String toEmail;
    private Map<String, String> metadata;
}
