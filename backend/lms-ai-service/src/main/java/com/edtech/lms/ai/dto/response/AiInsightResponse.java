package com.edtech.lms.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** AI Insight report returned to Company Admin or Super Admin dashboard. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightResponse {

    private String reportId;
    private Long courseId;
    private Long lessonId;
    private String insightScope;
    private String insightSummary;
    private String creatorSuggestion;
    private Integer sessionsAnalyzed;
    private LocalDateTime generatedAt;
}
