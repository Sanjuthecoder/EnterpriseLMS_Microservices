package com.edtech.lms.telemetry.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.edtech.lms.telemetry.models.XapiStatement;
import java.util.List;

@Repository
public interface XapiStatementRepository extends MongoRepository<XapiStatement, String> {
    List<XapiStatement> findByEmployeeIdAndCourseId(Long employeeId, Long courseId);
    List<XapiStatement> findByOrgIdAndCompanyIdAndEmployeeId(Long orgId, Long companyId, Long employeeId);
    // Fetch xAPI statements for a specific quiz type (PRE_QUIZ or POST_QUIZ) for uplift computation
    @Query("{ 'employeeId': ?0, 'courseId': ?1, 'context.extensions.quiz_type': ?2 }")
    List<XapiStatement> findByEmployeeIdAndCourseIdAndQuizType(Long employeeId, Long courseId, String quizType);
}
