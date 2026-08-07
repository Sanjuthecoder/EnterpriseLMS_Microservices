package com.edtech.lms.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cache.annotation.EnableCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableJpaRepositories(basePackages = "com.edtech.lms.course.repositories")
@EnableCaching
public class CourseServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(CourseServiceApplication.class);

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        logger.debug("Environment loaded");
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
