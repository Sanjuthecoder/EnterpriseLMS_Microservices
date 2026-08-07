package com.edtech.lms.telemetry.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking if MongoDB data migration from monolith collections is needed...");

        try {
            migrateCollection("video_telemetry", "svc_telemetry_video_telemetry");
            migrateCollection("xapi_statements", "svc_telemetry_xapi_statements");
            migrateCollection("telemetry_aggregations", "svc_telemetry_telemetry_aggregations");
        } catch (Exception e) {
            log.error("Data migration failed. Error: {}", e.getMessage());
        }
    }

    private void migrateCollection(String sourceCollection, String targetCollection) {
        if (!mongoTemplate.collectionExists(sourceCollection)) {
            log.info("Source collection {} does not exist. Skipping.", sourceCollection);
            return;
        }

        long count = mongoTemplate.getCollection(targetCollection).countDocuments();
        if (count == 0) {
            log.info("Collection {} is empty. Migrating data from {}...", targetCollection, sourceCollection);
            
            // Raw document copying without specific entity mapping to avoid class cast exceptions
            List<org.bson.Document> documents = mongoTemplate.findAll(org.bson.Document.class, sourceCollection);
            if (!documents.isEmpty()) {
                mongoTemplate.insert(documents, targetCollection);
                log.info("Successfully migrated {} documents to {}", documents.size(), targetCollection);
            } else {
                log.info("Source collection {} is empty. Nothing to migrate.", sourceCollection);
            }
        } else {
            log.info("Collection {} already contains data. Skipping migration.", targetCollection);
        }
    }
}
