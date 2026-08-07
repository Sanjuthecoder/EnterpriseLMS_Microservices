package com.edtech.lms.course.models.dtos;

import java.util.List;

/**
 * Response DTO for the aggregate video insight engine.
 * Generated from ALL video_telemetry documents for a given lessonId across all employees.
 */
public class VideoInsightResponse {

    private Long lessonId;
    private Integer totalSessions;

    // === Aggregate metrics ===
    private Double avgRewinds;
    private Double avgSkips;
    private Double avgPauses;
    private Double avgHighSpeedWatchPercent;
    private Double completionRate;

    // === Hotspot analysis ===
    private Integer confusionHotspotSeconds;
    private Integer confusionHotspotCount;
    private Integer boredomDropoffSeconds;
    private Integer boredomDropoffCount;

    // === Research-backed suggestions ===
    private List<InsightSuggestion> suggestions;

    // Constructors
    public VideoInsightResponse() {}

    public VideoInsightResponse(Long lessonId, Integer totalSessions, Double avgRewinds, Double avgSkips, 
                                Double avgPauses, Double avgHighSpeedWatchPercent, Double completionRate, 
                                Integer confusionHotspotSeconds, Integer confusionHotspotCount, 
                                Integer boredomDropoffSeconds, Integer boredomDropoffCount, 
                                List<InsightSuggestion> suggestions) {
        this.lessonId = lessonId;
        this.totalSessions = totalSessions;
        this.avgRewinds = avgRewinds;
        this.avgSkips = avgSkips;
        this.avgPauses = avgPauses;
        this.avgHighSpeedWatchPercent = avgHighSpeedWatchPercent;
        this.completionRate = completionRate;
        this.confusionHotspotSeconds = confusionHotspotSeconds;
        this.confusionHotspotCount = confusionHotspotCount;
        this.boredomDropoffSeconds = boredomDropoffSeconds;
        this.boredomDropoffCount = boredomDropoffCount;
        this.suggestions = suggestions;
    }

    // Getters and Setters
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

    public Double getAvgRewinds() { return avgRewinds; }
    public void setAvgRewinds(Double avgRewinds) { this.avgRewinds = avgRewinds; }

    public Double getAvgSkips() { return avgSkips; }
    public void setAvgSkips(Double avgSkips) { this.avgSkips = avgSkips; }

    public Double getAvgPauses() { return avgPauses; }
    public void setAvgPauses(Double avgPauses) { this.avgPauses = avgPauses; }

    public Double getAvgHighSpeedWatchPercent() { return avgHighSpeedWatchPercent; }
    public void setAvgHighSpeedWatchPercent(Double avgHighSpeedWatchPercent) { this.avgHighSpeedWatchPercent = avgHighSpeedWatchPercent; }

    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }

    public Integer getConfusionHotspotSeconds() { return confusionHotspotSeconds; }
    public void setConfusionHotspotSeconds(Integer confusionHotspotSeconds) { this.confusionHotspotSeconds = confusionHotspotSeconds; }

    public Integer getConfusionHotspotCount() { return confusionHotspotCount; }
    public void setConfusionHotspotCount(Integer confusionHotspotCount) { this.confusionHotspotCount = confusionHotspotCount; }

    public Integer getBoredomDropoffSeconds() { return boredomDropoffSeconds; }
    public void setBoredomDropoffSeconds(Integer boredomDropoffSeconds) { this.boredomDropoffSeconds = boredomDropoffSeconds; }

    public Integer getBoredomDropoffCount() { return boredomDropoffCount; }
    public void setBoredomDropoffCount(Integer boredomDropoffCount) { this.boredomDropoffCount = boredomDropoffCount; }

    public List<InsightSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<InsightSuggestion> suggestions) { this.suggestions = suggestions; }

    public static class InsightSuggestion {
        private String title;
        private String diagnosis;
        private String actionItem;
        private String evidenceStats;
        private String researchBasis;

        public InsightSuggestion() {}

        public InsightSuggestion(String title, String diagnosis, String actionItem, String evidenceStats, String researchBasis) {
            this.title = title;
            this.diagnosis = diagnosis;
            this.actionItem = actionItem;
            this.evidenceStats = evidenceStats;
            this.researchBasis = researchBasis;
        }

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDiagnosis() { return diagnosis; }
        public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

        public String getActionItem() { return actionItem; }
        public void setActionItem(String actionItem) { this.actionItem = actionItem; }

        public String getEvidenceStats() { return evidenceStats; }
        public void setEvidenceStats(String evidenceStats) { this.evidenceStats = evidenceStats; }

        public String getResearchBasis() { return researchBasis; }
        public void setResearchBasis(String researchBasis) { this.researchBasis = researchBasis; }
    }
}
