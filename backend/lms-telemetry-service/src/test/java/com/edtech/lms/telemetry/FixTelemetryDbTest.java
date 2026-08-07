package com.edtech.lms.telemetry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;

@SpringBootTest
public class FixTelemetryDbTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void migrateTelemetry() {
        try {
            System.out.println("----- CHECKING MONGODB SCHEMA -----");
            
            System.out.println("Collections available: " + mongoTemplate.getCollectionNames());
            
            migrate("video_telemetry", "svc_telemetry_video_telemetry");
            migrate("xapi_statements", "svc_telemetry_xapi_statements");
            migrate("telemetry_aggregations", "svc_telemetry_telemetry_aggregations");
            
            System.out.println("----- MIGRATION COMPLETE -----");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR QUERYING MONGODB: " + e.getMessage());
        }
    }
    
    private void migrate(String oldCol, String newCol) {
        if (!mongoTemplate.collectionExists(oldCol)) {
            System.out.println(oldCol + " does NOT exist. Skipping.");
            return;
        }
        
        long oldCount = mongoTemplate.getCollection(oldCol).countDocuments();
        System.out.println(oldCol + " has " + oldCount + " documents.");
        
        if (oldCount > 0) {
            System.out.println("Copying to " + newCol + "...");
            List<org.bson.Document> docs = mongoTemplate.findAll(org.bson.Document.class, oldCol);
            
            // Drop new collection to ensure clean copy
            if (mongoTemplate.collectionExists(newCol)) {
                mongoTemplate.dropCollection(newCol);
            }
            
            mongoTemplate.insert(docs, newCol);
            long newCount = mongoTemplate.getCollection(newCol).countDocuments();
            System.out.println(newCol + " now has " + newCount + " documents!");
        }
    }
}
