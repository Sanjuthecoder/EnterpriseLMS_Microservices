package com.edtech.lms.identity.services.notification;

import com.edtech.lms.identity.dtos.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Map;

@Service
public class NotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPublisher.class);
    private static final String TOPIC = "notification-topic";

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void sendOnboardingCredentials(String toEmail, String username, String temporaryPassword, String roleName) {
        if (toEmail == null || !toEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + toEmail);
        }
        
        NotificationEvent event = new NotificationEvent();
        event.setEventType("ONBOARDING");
        event.setToEmail(toEmail);
        event.setMetadata(Map.of(
            "username", username != null ? username : "",
            "password", temporaryPassword != null ? temporaryPassword : "",
            "roleName", roleName != null ? roleName : ""
        ));
            
        kafkaTemplate.send(TOPIC, toEmail, event);
        logger.info("Published ONBOARDING email event for {}", toEmail);
    }

    public void sendPasswordResetCode(String toEmail, String username, String verificationCode) {
        if (toEmail == null || !toEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + toEmail);
        }
        
        NotificationEvent event = new NotificationEvent();
        event.setEventType("PASSWORD_RESET");
        event.setToEmail(toEmail);
        event.setMetadata(Map.of(
            "username", username != null ? username : "",
            "verificationCode", verificationCode != null ? verificationCode : ""
        ));
            
        kafkaTemplate.send(TOPIC, toEmail, event);
        logger.info("Published PASSWORD_RESET email event for {}", toEmail);
    }
    
    public void sendEmail(String to, String subject, String body) {
        throw new UnsupportedOperationException("sendEmail is no longer supported directly. Use specific event methods.");
    }
}
