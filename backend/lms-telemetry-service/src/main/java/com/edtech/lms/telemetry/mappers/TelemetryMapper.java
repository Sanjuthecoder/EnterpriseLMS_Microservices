package com.edtech.lms.telemetry.mappers;

import com.edtech.lms.telemetry.models.TelemetryAggregation;
import com.edtech.lms.telemetry.models.VideoTelemetry;
import com.edtech.lms.telemetry.models.XapiStatement;
import com.edtech.lms.telemetry.models.dtos.responses.TelemetryAggregationResponse;
import com.edtech.lms.telemetry.models.dtos.responses.VideoTelemetryResponse;
import com.edtech.lms.telemetry.models.dtos.responses.XapiStatementResponse;

public class TelemetryMapper {

    public static XapiStatementResponse toXapiStatementResponse(XapiStatement entity) {
        if (entity == null) return null;
        return XapiStatementResponse.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .companyId(entity.getCompanyId())
                .employeeId(entity.getEmployeeId())
                .courseId(entity.getCourseId())
                .actor(entity.getActor())
                .verb(entity.getVerb())
                .object(entity.getObject())
                .result(entity.getResult())
                .context(entity.getContext())
                .timestamp(entity.getTimestamp())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static VideoTelemetryResponse toVideoTelemetryResponse(VideoTelemetry entity) {
        if (entity == null) return null;
        return VideoTelemetryResponse.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .companyId(entity.getCompanyId())
                .employeeId(entity.getEmployeeId())
                .courseId(entity.getCourseId())
                .lessonId(entity.getLessonId())
                .sessionId(entity.getSessionId())
                .events(entity.getEvents())
                .totalSeeks(entity.getTotalSeeks())
                .highSpeedSeconds(entity.getHighSpeedSeconds())
                .totalWatchTime(entity.getTotalWatchTime())
                .videoDuration(entity.getVideoDuration())
                .completionStatus(entity.getCompletionStatus())
                .completionPercentage(entity.getCompletionPercentage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static TelemetryAggregationResponse toTelemetryAggregationResponse(TelemetryAggregation entity) {
        if (entity == null) return null;
        return TelemetryAggregationResponse.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .companyId(entity.getCompanyId())
                .employeeId(entity.getEmployeeId())
                .courseId(entity.getCourseId())
                .metricType(entity.getMetricType())
                .period(entity.getPeriod())
                .patterns(entity.getPatterns())
                .suggestions(entity.getSuggestions())
                .recommendations(entity.getRecommendations())
                .metrics(entity.getMetrics())
                .computedAt(entity.getComputedAt())
                .build();
    }
}
