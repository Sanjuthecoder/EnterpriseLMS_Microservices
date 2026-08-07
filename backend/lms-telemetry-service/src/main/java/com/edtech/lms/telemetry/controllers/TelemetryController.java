package com.edtech.lms.telemetry.controllers;

import com.edtech.lms.telemetry.models.dtos.requests.XapiStatementRequest;
import com.edtech.lms.telemetry.models.dtos.responses.TelemetryAggregationResponse;
import com.edtech.lms.telemetry.models.dtos.responses.VideoTelemetryResponse;
import com.edtech.lms.telemetry.models.dtos.responses.XapiStatementResponse;
import com.edtech.lms.telemetry.mappers.TelemetryMapper;
import com.edtech.lms.telemetry.services.TelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    /**
     * Retrieves xAPI statements for a specific employee and course.
     *
     * @param employeeId the ID of the employee
     * @param courseId the ID of the course
     * @param quizType optional quiz type to filter statements
     * @return a list of xAPI statements
     */
    @GetMapping("/xapi-statements")
    public ResponseEntity<List<XapiStatementResponse>> getXapiStatements(
            @RequestParam Long employeeId,
            @RequestParam Long courseId,
            @RequestParam(required = false) String quizType) {
        
        List<XapiStatementResponse> statements = telemetryService.getXapiStatements(employeeId, courseId, quizType)
                .stream()
                .map(TelemetryMapper::toXapiStatementResponse)
                .toList();
        return ResponseEntity.ok(statements);
    }

    /**
     * Saves a new xAPI statement.
     *
     * @param request the xAPI statement request payload
     * @return the saved xAPI statement response
     */
    @PostMapping("/xapi-statements")
    public ResponseEntity<XapiStatementResponse> saveXapiStatement(@RequestBody XapiStatementRequest request) {
        com.edtech.lms.telemetry.models.XapiStatement saved = telemetryService.saveXapiStatement(request);
        return ResponseEntity.ok(TelemetryMapper.toXapiStatementResponse(saved));
    }

    /**
     * Saves or updates a video telemetry session.
     *
     * @param employeeId the ID of the employee from headers
     * @param companyId the ID of the company from headers
     * @param orgId the ID of the tenant/org from headers (optional)
     * @param payload the JSON payload containing video telemetry data
     * @return the saved video telemetry response
     */
    @PostMapping("/video-sessions")
    public ResponseEntity<VideoTelemetryResponse> saveVideoSession(
            @RequestHeader(value = "X-User-Id") Long employeeId,
            @RequestHeader(value = "X-Company-Id") Long companyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long orgId,
            @RequestBody com.fasterxml.jackson.databind.JsonNode payload) {
        
        com.edtech.lms.telemetry.models.VideoTelemetry saved = telemetryService.saveVideoSession(employeeId, companyId, orgId, payload);
        return ResponseEntity.ok(TelemetryMapper.toVideoTelemetryResponse(saved));
    }

    /**
     * Retrieves video telemetry sessions for a specific employee and course.
     *
     * @param employeeId the ID of the employee
     * @param courseId the ID of the course
     * @return a list of video telemetry sessions
     */
    @GetMapping("/video-sessions")
    public ResponseEntity<List<VideoTelemetryResponse>> getVideoSessions(
            @RequestParam Long employeeId,
            @RequestParam Long courseId) {
        
        List<VideoTelemetryResponse> sessions = telemetryService.getVideoSessions(employeeId, courseId)
                .stream()
                .map(TelemetryMapper::toVideoTelemetryResponse)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieves video telemetry sessions for a specific lesson.
     *
     * @param lessonId the ID of the lesson
     * @return a list of video telemetry sessions for the lesson
     */
    @GetMapping("/insights/lessons/{lessonId}/video-sessions")
    public ResponseEntity<List<VideoTelemetryResponse>> getLessonVideoSessions(@PathVariable Long lessonId) {
        List<VideoTelemetryResponse> sessions = telemetryService.getLessonVideoSessions(lessonId)
                .stream()
                .map(TelemetryMapper::toVideoTelemetryResponse)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieves aggregated telemetry data for a specific course.
     *
     * @param courseId the ID of the course
     * @return a list of telemetry aggregations
     */
    @GetMapping("/aggregations/courses/{courseId}")
    public ResponseEntity<List<TelemetryAggregationResponse>> getCourseAggregations(@PathVariable Long courseId) {
        List<TelemetryAggregationResponse> aggregations = telemetryService.getCourseAggregations(courseId)
                .stream()
                .map(TelemetryMapper::toTelemetryAggregationResponse)
                .toList();
        return ResponseEntity.ok(aggregations);
    }
}
