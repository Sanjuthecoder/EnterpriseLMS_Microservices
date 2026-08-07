package com.edtech.lms.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LmsTelemetryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LmsTelemetryServiceApplication.class, args);
    }
}
