package com.edtech.lms.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class LmsEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsEurekaServerApplication.class, args);
    }
}
