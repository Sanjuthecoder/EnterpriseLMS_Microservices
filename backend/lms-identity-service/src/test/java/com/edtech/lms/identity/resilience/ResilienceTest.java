package com.edtech.lms.identity.resilience;

import com.edtech.lms.identity.services.bulk.CsvImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.sql.SQLException;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResilienceTest {

    @Autowired
    private CsvImportService csvImportService;

    @Test
    public void testCsvImportRetry() {
        // Will throw simulated SQLException 50% of time, but Resilience4j will retry or hit fallback
        try {
            String result = csvImportService.importEmployees(Collections.singletonList("test@emp.com"));
            assertTrue("SUCCESS".equals(result) || "FAILED_BUT_HANDLED".equals(result));
        } catch (SQLException e) {
            fail("Exception should have been handled by fallback or retry");
        }
    }
}
