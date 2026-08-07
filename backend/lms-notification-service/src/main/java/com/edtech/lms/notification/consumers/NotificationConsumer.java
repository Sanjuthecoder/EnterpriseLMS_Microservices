package com.edtech.lms.notification.consumers;

import com.edtech.lms.notification.dtos.NotificationEvent;
import com.edtech.lms.notification.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer component responsible for listening to Kafka events on the notification topic.
 * 
 * Extracts the payload from the NotificationEvent and delegates
 * the actual sending process to the EmailService.
 */
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);
    private final EmailService emailService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * Listens to the 'notification-topic' and processes incoming NotificationEvents.
     * 
     * @param payload The raw JSON string payload.
     */
    @KafkaListener(topics = "notification-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNotificationEvent(String payload) {
        logger.info("Received raw notification payload: {}", payload);
        try {
            processEvent(objectMapper.readValue(payload, NotificationEvent.class));
        } catch (IllegalArgumentException e) {
            logger.error("Validation error processing notification payload {}: {}", payload, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing notification payload {}: {}", payload, e.getMessage());
        }
    }

    private void processEvent(NotificationEvent event) {
        logger.info("Successfully deserialized event of type: {}", event.getEventType());
        switch (event.getEventType()) {
            case "ONBOARDING" -> emailService.sendOnboardingCredentials(
                event.getToEmail(),
                event.getMetadata().get("username"),
                event.getMetadata().get("password"),
                event.getMetadata().get("roleName")
            );
            case "PASSWORD_RESET" -> emailService.sendPasswordResetCode(
                event.getToEmail(),
                event.getMetadata().get("username"),
                event.getMetadata().get("verificationCode")
            );
            case "PREMIUM_UPGRADE" -> emailService.sendPremiumUpgradeConfirmation(
                event.getToEmail(),
                event.getMetadata().get("username")
            );
            default -> logger.warn("Unknown event type received: {}", event.getEventType());
        }
    }
}
