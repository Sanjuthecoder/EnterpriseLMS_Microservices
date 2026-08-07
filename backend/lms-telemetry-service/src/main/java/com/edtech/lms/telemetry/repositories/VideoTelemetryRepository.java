package com.edtech.lms.telemetry.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.edtech.lms.telemetry.models.VideoTelemetry;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoTelemetryRepository extends MongoRepository<VideoTelemetry, String> {
    List<VideoTelemetry> findByEmployeeIdAndCourseId(Long employeeId, Long courseId);
    List<VideoTelemetry> findByOrgIdAndCompanyIdAndEmployeeId(Long orgId, Long companyId, Long employeeId);
    // For aggregate video insight engine — fetches all sessions for a lesson across all companies
    List<VideoTelemetry> findByLessonId(Long lessonId);
    // Per employee per lesson — may have multiple sessions (sessionId is unique per play)
    List<VideoTelemetry> findByEmployeeIdAndLessonId(Long employeeId, Long lessonId);
    Optional<VideoTelemetry> findByEmployeeIdAndLessonIdAndSessionId(Long employeeId, Long lessonId, String sessionId);
}
