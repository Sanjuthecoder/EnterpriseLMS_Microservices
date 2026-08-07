package com.edtech.lms.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
public class FixDbTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.edtech.lms.identity.repositories.UserRepository userRepository;

    @Test
    public void fixStatus() {
        try {
            System.out.println("----- PROPERLY MIGRATING ORGS AND COMPANIES WITH COLUMN MAPPING -----");
            
            String orgCols = "org_id, created_at, email, free_courses, free_employees, name, status, updated_at";
            jdbcTemplate.execute("INSERT IGNORE INTO svc_identity_organizations (" + orgCols + ") " +
                                 "SELECT " + orgCols + " FROM organizations");
            
            String compCols = "company_id, admin_email, created_at, logo_url, name, org_id, status, theme_config, updated_at";
            jdbcTemplate.execute("INSERT IGNORE INTO svc_identity_companies (" + compCols + ") " +
                                 "SELECT " + compCols + " FROM companies");
                                 
            System.out.println("----- VERIFYING TOTALS NOW -----");
            Integer orgCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM svc_identity_organizations", Integer.class);
            Integer compCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM svc_identity_companies", Integer.class);
            System.out.println("Total organizations now: " + orgCount);
            System.out.println("Total companies now: " + compCount);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR QUERYING DATABASE: " + e.getMessage());
        }
    }
}
