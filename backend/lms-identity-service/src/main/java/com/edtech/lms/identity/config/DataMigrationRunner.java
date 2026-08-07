package com.edtech.lms.identity.config;

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
            migrateTable("organizations", "svc_identity_organizations");
            migrateTable("companies", "svc_identity_companies");
            migrateTable("users", "svc_identity_users");
        } catch (Exception e) {
            log.error("Data migration failed or tables do not exist yet. This is expected on first boot before schemas are generated. Error: {}", e.getMessage());
        }

        // Clean up invalid roles that may have migrated from monolith
        try {
            jdbcTemplate.execute("UPDATE svc_identity_users SET role = 'SUPER_ADMIN' WHERE role = '' OR role IS NULL OR role = 'super_admin' OR role = 'SUPERADMIN'");
            jdbcTemplate.execute("UPDATE svc_identity_users SET role = 'COMPANY_ADMIN' WHERE role = 'company_admin'");
            jdbcTemplate.execute("UPDATE svc_identity_users SET role = 'EMPLOYEE' WHERE role = 'employee'");
            
            // Clean up invalid status that may have migrated from monolith
            jdbcTemplate.execute("UPDATE svc_identity_users SET status = 'ACTIVE' WHERE status = '' OR status IS NULL OR status = 'active' OR status = 'ACTIVE '");
            
            log.info("Sanitized invalid user roles and statuses in database.");
        } catch (Exception e) {
            log.warn("Failed to sanitize roles: {}", e.getMessage());
        }
    }

    private void migrateTable(String sourceTable, String targetTable) {
        // Check if target table is empty
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + targetTable, Integer.class);
        if (count != null && count == 0) {
            log.info("Table {} is empty. Migrating data from {}...", targetTable, sourceTable);
            // Copy data over. IGNORE prevents failure if data is somehow duplicated.
            jdbcTemplate.execute("INSERT IGNORE INTO " + targetTable + " SELECT * FROM " + sourceTable);
            log.info("Successfully migrated data to {}", targetTable);
        } else {
            log.info("Table {} already contains data. Skipping migration.", targetTable);
        }
    }
}
