package com.edtech.lms.telemetry.config;

import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MongoMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MongoMigrationRunner.class);

    private final MongoTemplate mongoTemplate;

    public MongoMigrationRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void run(String... args) throws Exception {
        logger.info("Starting MongoDB Data Migration...");
        
        try {
            // 1. Migrate video_telemetry
            List<Document> videoDocs = mongoTemplate.findAll(Document.class, "video_telemetry");
            if (!videoDocs.isEmpty()) {
                logger.info("Migrating {} video_telemetry documents.", videoDocs.size());
                for (Document doc : videoDocs) {
                    mongoTemplate.save(doc, "svc_telemetry_video_telemetry");
                }
            } else {
                logger.info("No video_telemetry data to migrate.");
            }
            
            // 2. Migrate xapi_statements
            List<Document> xapiDocs = mongoTemplate.findAll(Document.class, "xapi_statements");
            if (!xapiDocs.isEmpty()) {
                logger.info("Migrating {} xapi_statements documents.", xapiDocs.size());
                for (Document doc : xapiDocs) {
                    mongoTemplate.save(doc, "svc_telemetry_xapi_statements");
                }
            } else {
                logger.info("No xapi_statements data to migrate.");
            }
            
            logger.info("MongoDB Data Migration Completed Successfully.");
            
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }
}
