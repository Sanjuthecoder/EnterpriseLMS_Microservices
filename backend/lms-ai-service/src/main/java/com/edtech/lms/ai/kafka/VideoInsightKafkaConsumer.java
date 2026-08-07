package com.edtech.lms.ai.kafka;

import com.edtech.lms.ai.service.AiInsightService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInsightKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final AiInsightService aiInsightService;

    @KafkaListener(topics = "video-insight-request-topic", groupId = "ai-service-group")
    public void consumeVideoInsightRequest(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            log.info("Received video insight generation request for lessonId={}", payload.get("lessonId"));
            
            aiInsightService.processInsightRequest(payload);
            
        } catch (Exception e) {
            log.error("Failed to process video insight request: {}", e.getMessage(), e);
        }
    }
}
