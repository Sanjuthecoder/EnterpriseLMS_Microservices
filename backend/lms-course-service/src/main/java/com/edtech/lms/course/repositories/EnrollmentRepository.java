package com.edtech.lms.course.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.enums.EnrollmentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByEmployeeIdAndCompanyId(String employeeId, String companyId);
    List<Enrollment> findByCourseIdAndCompanyId(Long courseId, String companyId);
    Optional<Enrollment> findByEmployeeIdAndCourseIdAndCompanyId(String employeeId, Long courseId, String companyId);
    List<Enrollment> findByCompanyId(String companyId);
    List<Enrollment> findByCourseId(Long courseId);
    
    // For Saga Pattern deletions
    @Modifying
    @Transactional
    @Query("DELETE FROM Enrollment e WHERE e.companyId = :companyId")
    void deleteByCompanyId(@Param("companyId") String companyId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Enrollment e WHERE e.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") String employeeId);

    long countByCompanyIdAndStatus(String companyId, EnrollmentStatus status);
    long countByCourseId(Long courseId);
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    
    @Query("SELECT COUNT(e) FROM Enrollment e JOIN Course c ON e.courseId = c.courseId WHERE c.creatorId = :creatorId AND e.status IN :statuses")
    long countByCreatorIdAndStatuses(@Param("creatorId") String creatorId, @Param("statuses") Collection<EnrollmentStatus> statuses);

    @Query("SELECT AVG(e.progressPercentage) FROM Enrollment e WHERE e.companyId = :companyId")
    Double getAverageCompletionRateByCompanyId(@Param("companyId") String companyId);
}
