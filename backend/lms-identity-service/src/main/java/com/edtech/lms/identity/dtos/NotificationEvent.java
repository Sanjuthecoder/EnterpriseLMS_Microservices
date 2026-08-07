package com.edtech.lms.identity.dtos;

import java.util.Map;

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

    public static NotificationEventBuilder builder() {
        return new NotificationEventBuilder();
    }

    public static class NotificationEventBuilder {
        private String eventType;
        private String toEmail;
        private Map<String, String> metadata;

        public NotificationEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public NotificationEventBuilder toEmail(String toEmail) {
            this.toEmail = toEmail;
            return this;
        }

        public NotificationEventBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public NotificationEvent build() {
            return new NotificationEvent(eventType, toEmail, metadata);
        }
    }
}
