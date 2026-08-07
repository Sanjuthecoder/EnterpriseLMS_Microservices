package com.edtech.lms.identity.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

/**
 * EnvConfig - Loads .env file at application startup
 * 
 * How it works:
 * 1. Spring Boot starts
 * 2. Before any beans are created, this ApplicationRunner runs
 * 3. It loads the .env file from the project root
 * 4. Each variable from .env is set as an environment variable
 * 5. Application.properties placeholders now resolve correctly
 * 6. JwtConfig and DatabaseConfig can access the values
 * 
 * Example .env file:
 * DB_MYSQL_URL=jdbc:mysql://...
 * JWT_SECRET_KEY=abc123...
 */
@Configuration
public class EnvConfig {

    static {
        // Load .env file when this class is loaded
        // This happens before Spring creates any beans
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()  // Don't fail if .env doesn't exist
                .load();

        // Set each .env variable as system property
        // This allows ${VAR_NAME} placeholders to resolve
        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    /**
     * This bean is intentionally empty
     * It just ensures EnvConfig is instantiated and static block runs
     */
    @Bean
    public EnvConfigInitializer envConfigInitializer() {
        return new EnvConfigInitializer();
    }

    public static class EnvConfigInitializer {
        // Marker class to ensure EnvConfig is loaded
    }
}
