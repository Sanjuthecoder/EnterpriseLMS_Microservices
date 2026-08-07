package com.edtech.lms.ai.repository;

import com.edtech.lms.ai.entity.AiInsightReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * AiInsightReportRepository — Data access for AI-generated insight reports.
 *
 * Database operations only. No business logic here.
 */
public interface AiInsightReportRepository extends MongoRepository<AiInsightReport, String> {

    /**
     * Fetch all company-scoped insight reports for a specific course and company.
     * Used by the Company Admin dashboard.
     */
    List<AiInsightReport> findByCourseIdAndCompanyIdAndInsightScope(
            Long courseId, String companyId, String insightScope);

    /**
     * Fetch platform-scoped (cross-company) insights for a course.
     * Used by Super Admins and shared with content creators.
     */
    List<AiInsightReport> findByCourseIdAndInsightScope(Long courseId, String insightScope);

    /**
     * Delete stale reports before regenerating during nightly batch.
     * Prevents duplicate reports for the same lesson/company.
     */
    void deleteByLessonIdAndCompanyIdAndInsightScope(
            Long lessonId, String companyId, String insightScope);

    /**
     * Fetch all lesson-level reports for batch processing scope.
     */
    List<AiInsightReport> findByLessonIdAndInsightScope(Long lessonId, String insightScope);
}
