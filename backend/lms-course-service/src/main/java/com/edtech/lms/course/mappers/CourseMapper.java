package com.edtech.lms.course.mappers;

import com.edtech.lms.course.models.dtos.CourseResponse;
import com.edtech.lms.course.models.dtos.EnrollmentResponse;
import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.Enrollment;

/**
 * Mapper utility for Course Service.
 */
public class CourseMapper {

    private CourseMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static CourseResponse toCourseResponse(Course entity) {
        if (entity == null) {
            return null;
        }

        return CourseResponse.builder()
                .courseId(entity.getCourseId())
                .orgId(entity.getOrgId())
                .creatorId(entity.getCreatorId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .difficultyLevel(entity.getDifficultyLevel())
                .status(entity.getStatus())
                .durationMinutes(entity.getDurationMinutes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static EnrollmentResponse toEnrollmentResponse(Enrollment entity) {
        if (entity == null) {
            return null;
        }

        return EnrollmentResponse.builder()
                .enrollmentId(entity.getEnrollmentId())
                .companyId(entity.getCompanyId())
                .employeeId(entity.getEmployeeId())
                .courseId(entity.getCourseId())
                .status(entity.getStatus())
                .assignedDate(entity.getAssignedDate())
                .deadline(entity.getDeadline())
                .completionDate(entity.getCompletionDate())
                .progressPercentage(entity.getProgressPercentage())
                .score(entity.getScore())
                .recommendations(entity.getRecommendations())
                .lessonGatingMap(entity.getLessonGatingMap())
                .completedLessons(entity.getCompletedLessons())
                .preQuizScore(entity.getPreQuizScore())
                .postQuizScore(entity.getPostQuizScore())
                .upliftPercent(entity.getUpliftPercent())
                .upliftReport(entity.getUpliftReport())
                .certificateStatus(entity.getCertificateStatus())
                .build();
    }
}
