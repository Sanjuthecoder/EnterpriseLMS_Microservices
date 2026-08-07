package com.edtech.lms.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LmsAiServiceApplication — Entry point for the AI-Powered Learning Personalization Service.
 *
 * Premium-only feature. Enforced at both API Gateway (JWT claim check) and
 * method-level security within this service.
 *
 * Key responsibilities:
 * - Pre-quiz and Post-quiz generation via Google Gemini API
 * - Dynamic learning path computation from xAPI quiz results
 * - Video telemetry aggregation for Company Admin / Super Admin insights
 * - Nightly batch job for AI insight generation
 * - Course content ingestion (fetches existing courses from lms-course-service)
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class LmsAiServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LmsAiServiceApplication.class, args);
    }
}
