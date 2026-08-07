package com.edtech.lms.identity.services.bulk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;

import java.sql.SQLException;
import java.util.List;

@Service
public class CsvImportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportService.class);

    @Retry(name = "bulkImport", fallbackMethod = "importFallback")
    public String importEmployees(List<String> csvLines) throws SQLException {
        logger.info("Starting bulk import of {} employees...", csvLines.size());

        // Simulate a database deadlock or transient failure
        if (Math.random() > 0.5) {
            logger.error("Simulated DB Deadlock during import!");
            throw new SQLException("Deadlock found when trying to get lock; try restarting transaction");
        }

        logger.info("Successfully imported all employees.");
        return "SUCCESS";
    }

    public String importFallback(List<String> csvLines, Throwable t) {
        logger.warn("Bulk import failed after retries for {} lines. Error: {}", csvLines.size(), t.getMessage());
        return "FAILED_BUT_HANDLED";
    }
}
