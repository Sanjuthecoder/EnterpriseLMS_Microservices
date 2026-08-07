package com.edtech.lms.telemetry.services;

import com.edtech.lms.telemetry.models.TelemetryAggregation;
import com.edtech.lms.telemetry.models.VideoTelemetry;
import com.edtech.lms.telemetry.models.XapiStatement;
import com.edtech.lms.telemetry.models.dtos.requests.XapiStatementRequest;
import com.edtech.lms.telemetry.repositories.TelemetryAggregationRepository;
import com.edtech.lms.telemetry.repositories.VideoTelemetryRepository;
import com.edtech.lms.telemetry.repositories.XapiStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final XapiStatementRepository xapiStatementRepository;
    private final VideoTelemetryRepository videoTelemetryRepository;
    private final TelemetryAggregationRepository telemetryAggregationRepository;

    /**
     * Retrieves xAPI statements for a specific employee and course.
     *
     * @param employeeId the ID of the employee
     * @param courseId the ID of the course
     * @param quizType optional quiz type to filter statements
     * @return a list of xAPI statements
     */
    public List<XapiStatement> getXapiStatements(Long employeeId, Long courseId, String quizType) {
        if (quizType != null) {
            return xapiStatementRepository.findByEmployeeIdAndCourseIdAndQuizType(employeeId, courseId, quizType);
        }
        return xapiStatementRepository.findByEmployeeIdAndCourseId(employeeId, courseId);
    }

    /**
     * Saves a new xAPI statement.
     *
     * @param request the xAPI statement request payload
     * @return the saved xAPI statement
     */
    public XapiStatement saveXapiStatement(XapiStatementRequest request) {
        XapiStatement statement = new XapiStatement();
        statement.setCompanyId(request.getCompanyId());
        statement.setEmployeeId(request.getEmployeeId());
        statement.setCourseId(request.getCourseId());
        statement.setActor(request.getActor());
        statement.setVerb(request.getVerb());
        statement.setObject(request.getObject());
        statement.setResult(request.getResult());
        statement.setContext(request.getContext());
        statement.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        
        return xapiStatementRepository.save(statement);
    }

    /**
     * Saves or updates a video telemetry session.
     *
     * @param employeeId the ID of the employee
     * @param companyId the ID of the company
     * @param orgId the ID of the tenant/org
     * @param requestNode the JSON payload containing video telemetry data
     * @return the saved video telemetry session
     */
    public VideoTelemetry saveVideoSession(Long employeeId, Long companyId, Long orgId, com.fasterxml.jackson.databind.JsonNode requestNode) {
        Long courseId = requestNode.has("courseId") ? requestNode.get("courseId").asLong() : null;
        Long lessonId = requestNode.has("lessonId") ? requestNode.get("lessonId").asLong() : null;
        String sessionId = requestNode.has("sessionId") ? requestNode.get("sessionId").asText() : null;

        java.util.Optional<VideoTelemetry> existing = videoTelemetryRepository
                .findByEmployeeIdAndLessonIdAndSessionId(employeeId, lessonId, sessionId);

        java.util.List<com.fasterxml.jackson.databind.JsonNode> eventNodes = new java.util.ArrayList<>();
        if (requestNode.has("events") && requestNode.get("events").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode event : requestNode.get("events")) {
                eventNodes.add(event);
            }
        }

        VideoTelemetry telemetry = existing.isPresent() 
                ? updateExistingTelemetry(existing.get(), requestNode, eventNodes)
                : createNewTelemetry(employeeId, companyId, orgId, courseId, lessonId, sessionId, requestNode, eventNodes);

        return videoTelemetryRepository.save(telemetry);
    }

    private VideoTelemetry updateExistingTelemetry(VideoTelemetry telemetry, com.fasterxml.jackson.databind.JsonNode requestNode, java.util.List<com.fasterxml.jackson.databind.JsonNode> eventNodes) {
        telemetry.setEvents(eventNodes);
        if (requestNode.has("totalSeeks")) telemetry.setTotalSeeks(requestNode.get("totalSeeks").asInt());
        if (requestNode.has("highSpeedSeconds")) telemetry.setHighSpeedSeconds(requestNode.get("highSpeedSeconds").asInt());
        if (requestNode.has("completionPercentage")) telemetry.setCompletionPercentage(requestNode.get("completionPercentage").asInt());
        if (requestNode.has("completionStatus")) telemetry.setCompletionStatus(requestNode.get("completionStatus").asText());
        if (requestNode.has("duration")) telemetry.setVideoDuration(requestNode.get("duration").asInt());
        telemetry.setUpdatedAt(LocalDateTime.now());
        return telemetry;
    }

    private VideoTelemetry createNewTelemetry(Long employeeId, Long companyId, Long orgId, Long courseId, Long lessonId, String sessionId, com.fasterxml.jackson.databind.JsonNode requestNode, java.util.List<com.fasterxml.jackson.databind.JsonNode> eventNodes) {
        return VideoTelemetry.builder()
                .orgId(orgId != null ? orgId : companyId)
                .companyId(companyId)
                .employeeId(employeeId)
                .courseId(courseId)
                .lessonId(lessonId)
                .sessionId(sessionId)
                .events(eventNodes)
                .totalSeeks(requestNode.has("totalSeeks") ? requestNode.get("totalSeeks").asInt() : 0)
                .highSpeedSeconds(requestNode.has("highSpeedSeconds") ? requestNode.get("highSpeedSeconds").asInt() : 0)
                .videoDuration(requestNode.has("duration") ? requestNode.get("duration").asInt() : 0)
                .completionPercentage(requestNode.has("completionPercentage") ? requestNode.get("completionPercentage").asInt() : 0)
                .completionStatus(requestNode.has("completionStatus") ? requestNode.get("completionStatus").asText() : "started")
                .build();
    }

    /**
     * Retrieves video telemetry sessions for a specific employee and course.
     *
     * @param employeeId the ID of the employee
     * @param courseId the ID of the course
     * @return a list of video telemetry sessions
     */
    public List<VideoTelemetry> getVideoSessions(Long employeeId, Long courseId) {
        return videoTelemetryRepository.findByEmployeeIdAndCourseId(employeeId, courseId);
    }

    /**
     * Retrieves video telemetry sessions for a specific lesson.
     *
     * @param lessonId the ID of the lesson
     * @return a list of video telemetry sessions for the lesson
     */
    public List<VideoTelemetry> getLessonVideoSessions(Long lessonId) {
        return videoTelemetryRepository.findByLessonId(lessonId);
    }

    /**
     * Retrieves aggregated telemetry data for a specific course.
     *
     * @param courseId the ID of the course
     * @return a list of telemetry aggregations
     */
    public List<TelemetryAggregation> getCourseAggregations(Long courseId) {
        return telemetryAggregationRepository.findByCourseId(courseId);
    }
}
