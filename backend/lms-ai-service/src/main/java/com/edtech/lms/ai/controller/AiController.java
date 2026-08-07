package com.edtech.lms.ai.controller;

import com.edtech.lms.ai.dto.response.AiInsightResponse;
import com.edtech.lms.ai.service.AiInsightService;
import com.edtech.lms.ai.service.AiQuizService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AiController — REST API for AI-powered learning personalization features.
 *
 * <h2>Architectural change (Phase 2 migration)</h2>
 * <p>The pre-quiz and post-quiz GET/POST endpoints have been <strong>removed</strong>.
 * Quiz retrieval and submission are now handled entirely by {@code EmployeeController}
 * in {@code lms-course-service}, which performs tier-based smart routing using the
 * {@code isAiGenerated} flag on the {@link com.edtech.lms.ai.entity.CourseAiContext}.
 *
 * <p>This controller now exposes only:
 * <ul>
 *   <li>AI Insight reports (Company Admin / Super Admin) — unchanged.</li>
 *   <li>Admin endpoints for manual quiz backfill and re-generation.</li>
 * </ul>
 *
 * <p>All endpoints are under {@code /api/v1/ai/} and are restricted to Premium subscribers
 * by the gateway-level JWT Premium claim check and {@code PremiumTierFilter}.
 *
 * @see AiInsightService

 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiQuizService aiQuizService;
    private final AiInsightService aiInsightService;


    // =========================================================================
    // INSIGHTS (Company Admin)
    // =========================================================================

    /**
     * Returns AI insight reports for a course scoped to the requesting company.
     * For Company Admin dashboards.
     *
     * @param courseId  the course to get insights for
     * @param companyId company scope from request header
     * @return 200 OK with list of insight reports
     */
    @GetMapping("/insights/company/courses/{courseId}")
    public ResponseEntity<List<AiInsightResponse>> getCompanyInsights(
            @PathVariable final Long courseId,
            @RequestHeader("X-Company-Id") final String companyId) {

        log.info("Company insight request: courseId={}, companyId={}", courseId, companyId);
        return ResponseEntity.ok(aiInsightService.getCompanyInsights(courseId, companyId));
    }

    // =========================================================================
    // INSIGHTS (Super Admin — Platform level)
    // =========================================================================

    /**
     * Returns cross-company platform-level insights for a course.
     * For Super Admin dashboards — shareable with content creators.
     *
     * @param courseId the course to get insights for
     * @return 200 OK with list of insight reports
     */
    @GetMapping("/insights/platform/courses/{courseId}")
    public ResponseEntity<List<AiInsightResponse>> getPlatformInsights(
            @PathVariable final Long courseId) {

        log.info("Platform insight request: courseId={}", courseId);
        return ResponseEntity.ok(aiInsightService.getPlatformInsights(courseId));
    }

    // =========================================================================
    // ADMIN — Internal Trigger Endpoints
    // =========================================================================



    /**
     * Manually triggers quiz generation and publishing for a single specific course.
     * Use this for surgical re-generation when a course's quizzes need refreshing.
     *
     * @param courseId the course to re-generate quizzes for
     * @return 200 OK confirmation message
     */
    @PostMapping("/admin/courses/{courseId}/generate-quizzes")
    public ResponseEntity<String> triggerQuizGeneration(@PathVariable final Long courseId) {
        log.info("Admin trigger: generating quizzes for courseId={}", courseId);
        aiQuizService.generateAndPublishCourseQuizzes(courseId);
        return ResponseEntity.ok("Quiz generation triggered for courseId=" + courseId);
    }
}
