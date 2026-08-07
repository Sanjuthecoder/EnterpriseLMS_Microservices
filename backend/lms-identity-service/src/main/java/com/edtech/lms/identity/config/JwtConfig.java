package com.edtech.lms.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * JwtConfig - Loads JWT configuration from application.properties
 * 
 * How Spring Boot handles this:
 * 1. Reads app.jwt.secret and app.jwt.expiration from application.properties
 * 2. Values come from .env variables
 * 3. Injects into JwtTokenProvider service
 * 
 * Purpose:
 * - Externalize JWT settings from code
 * - Allow different secrets for different environments (dev/staging/prod)
 * - Keep secrets out of git (stored in .env file)
 */
@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    /**
     * @Value annotation:
     * - Reads from application.properties using ${property.name} syntax
     * - If property uses ${ENV_VAR}, Spring looks in environment variables
     * - Throws exception if property not found and not required
     * 
     * Flow:
     * .env: JWT_SECRET_KEY=abc123...
     * ↓
     * application.properties: app.jwt.secret=${JWT_SECRET_KEY}
     * ↓
     * JwtConfig.secret = "abc123..."
     */
}
