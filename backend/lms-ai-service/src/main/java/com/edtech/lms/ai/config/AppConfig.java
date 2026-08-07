package com.edtech.lms.ai.config;

import com.edtech.lms.ai.filter.PremiumTierFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * AppConfig — Spring beans and security configuration for lms-ai-service.
 *
 * Security model:
 * - All AI endpoints are accessible only after gateway-level JWT validation.
 * - The AI service trusts the X-Employee-Id and X-Company-Id headers forwarded by the gateway.
 * - CSRF is disabled (stateless REST API with JWT).
 * - Session is stateless (NEVER creates HTTP sessions).
 *
 * Note: We do NOT validate the JWT again here (the gateway already did it).
 * This follows the "trust the gateway" pattern used by all other services in this project.
 */
@Configuration
@EnableWebSecurity
public class AppConfig {

    /**
     * Permits all requests (gateway already validates JWT before forwarding).
     * This service trusts the forwarded headers: X-Employee-Id, X-Company-Id.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll()
                )
                .build();
    }

    /** RestTemplate used by GeminiClient and VideoInsightService for HTTP calls. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Registers PremiumTierFilter as a Servlet filter (not Spring Security filter).
     * Order 1 ensures it runs before Spring Security's filter chain.
     */
    @Bean
    public FilterRegistrationBean<PremiumTierFilter> premiumTierFilterRegistration(
            final PremiumTierFilter premiumTierFilter) {
        final FilterRegistrationBean<PremiumTierFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(premiumTierFilter);
        registration.addUrlPatterns("/api/v1/ai/*", "/api/v1/ai/**");
        registration.setOrder(1);
        registration.setName("premiumTierFilter");
        return registration;
    }

    /** ObjectMapper configured to handle Java 8 date/time types from MongoDB documents. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
