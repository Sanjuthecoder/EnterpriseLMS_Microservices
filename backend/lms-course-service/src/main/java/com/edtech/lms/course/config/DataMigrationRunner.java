package com.edtech.lms.course.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking if data migration from monolith tables is needed...");

        try {
            migrateTable("courses", "svc_course_courses");
            migrateTable("course_structure", "svc_course_course_structure");
            migrateTable("enrollments", "svc_course_enrollments");
            migrateTable("company_course_availability", "svc_course_company_course_availability");
            migrateTable("quiz_questions", "svc_course_quiz_questions");
        } catch (Exception e) {
            log.error("Data migration failed or tables do not exist yet. Error: {}", e.getMessage());
        }

        log.info("Running AI generated flag backfill...");
        
        long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM svc_course_quiz_questions", Long.class);
        if (count == 0) {
            log.info("Table svc_course_quiz_questions is empty. Proceeding with migration...");
            jdbcTemplate.execute("UPDATE svc_course_quiz_questions SET is_ai_generated = false WHERE is_ai_generated IS NULL");
        } else {
            log.info("Table svc_course_quiz_questions already contains data. Skipping migration.");
        }

        try {
            log.info("Creating index idx_quiz_course_type_ai...");
            jdbcTemplate.execute("CREATE INDEX idx_quiz_course_type_ai ON svc_course_quiz_questions (course_id, quiz_type, is_ai_generated)");
            log.info("Index created successfully.");
        } catch (Exception e) {
            log.warn("Index error (might already exist): {}", e.getMessage());
        }

    }

    private void migrateTable(String sourceTable, String targetTable) {
        // Check if target table is empty
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + targetTable, Integer.class);
        if (count != null && count == 0) {
            log.info("Table {} is empty. Migrating data from {}...", targetTable, sourceTable);
            jdbcTemplate.execute("INSERT IGNORE INTO " + targetTable + " SELECT * FROM " + sourceTable);
            log.info("Successfully migrated data to {}", targetTable);
        } else {
            log.info("Table {} already contains data. Skipping migration.", targetTable);
        }
    }
}
