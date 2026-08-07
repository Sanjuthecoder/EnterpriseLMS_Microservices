package com.edtech.lms.course.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.edtech.lms.course.models.entities.Course;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    /**
     * Get all courses in an organization (created by any creator)
     */
    List<Course> findByOrgId(String orgId);
    
    /**
     * Get courses created by a specific creator
     */
    List<Course> findByCreatorId(String creatorId);
    
    /**
     * Get courses available for a specific company
     * Joins with CompanyCourseAvailability to filter by company access
     * 
     * @param companyId the company requesting courses
     * @return list of available courses for the company
     */
    @Query("SELECT c FROM Course c " +
           "INNER JOIN CompanyCourseAvailability cca ON c.courseId = cca.courseId " +
           "WHERE cca.companyId = :companyId AND cca.isAvailable = true " +
           "ORDER BY c.createdAt DESC")
    List<Course> findAvailableCoursesForCompany(@Param("companyId") String companyId);
}
