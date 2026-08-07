package com.edtech.lms.telemetry.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.edtech.lms.telemetry.models.TelemetryAggregation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryAggregationRepository extends MongoRepository<TelemetryAggregation, String> {
    List<TelemetryAggregation> findByEmployeeIdAndCourseIdAndPeriod(Long employeeId, Long courseId, LocalDate period);
    Optional<TelemetryAggregation> findByOrgIdAndCompanyIdAndEmployeeIdAndPeriod(Long orgId, Long companyId, Long employeeId, LocalDate period);
    List<TelemetryAggregation> findByCourseId(Long courseId);
}
