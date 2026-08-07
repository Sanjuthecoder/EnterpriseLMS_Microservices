package com.edtech.lms.course.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.enums.ContentType;
import java.util.List;

@Repository
public interface CourseStructureRepository extends JpaRepository<CourseStructure, Long> {
    List<CourseStructure> findByCourseIdOrderBySeqOrder(Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cs) FROM CourseStructure cs JOIN Course c ON cs.courseId = c.courseId WHERE c.creatorId = :creatorId AND cs.lessonType = :lessonType")
    long countLessonsByCreatorIdAndType(@org.springframework.data.repository.query.Param("creatorId") String creatorId, @org.springframework.data.repository.query.Param("lessonType") com.edtech.lms.course.models.enums.ContentType lessonType);
}
