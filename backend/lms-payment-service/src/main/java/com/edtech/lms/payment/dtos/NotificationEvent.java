package com.edtech.lms.payment.dtos;

import java.util.Map;

/**
 * Event published to Kafka when a notification needs to be sent.
 */
public class NotificationEvent {
    private String eventType;
    private String toEmail;
    private Map<String, String> metadata;

    public NotificationEvent() {}

    public NotificationEvent(String eventType, String toEmail, Map<String, String> metadata) {
        this.eventType = eventType;
        this.toEmail = toEmail;
        this.metadata = metadata;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
