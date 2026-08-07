package com.edtech.lms.course.repositories;

import com.edtech.lms.course.models.entities.CompanyCourseAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyCourseAvailabilityRepository extends JpaRepository<CompanyCourseAvailability, Long> {

    List<CompanyCourseAvailability> findByCompanyIdAndIsAvailableTrue(String companyId);

    Optional<CompanyCourseAvailability> findByCompanyIdAndCourseId(String companyId, Long courseId);

    List<CompanyCourseAvailability> findByCourseIdAndIsAvailableTrue(Long courseId);

    List<CompanyCourseAvailability> findByCourseId(Long courseId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM CompanyCourseAvailability c " +
           "WHERE c.companyId = :companyId AND c.courseId = :courseId AND c.isAvailable = true")
    Boolean isCourseAvailableForCompany(@Param("companyId") String companyId, @Param("courseId") Long courseId);

    List<CompanyCourseAvailability> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    
    // For Saga pattern deletions
    @Modifying
    @Transactional
    @Query("DELETE FROM CompanyCourseAvailability c WHERE c.companyId = :companyId")
    void deleteByCompanyId(@Param("companyId") String companyId);
}
