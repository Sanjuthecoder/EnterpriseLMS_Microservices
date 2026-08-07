package com.edtech.lms.ai.repository;

import com.edtech.lms.ai.entity.CourseAiContext;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * CourseAiContextRepository — Data access for AI course context documents.
 *
 * Database operations only. No business logic here.
 */
public interface CourseAiContextRepository extends MongoRepository<CourseAiContext, String> {

    /**
     * Retrieve existing context by courseId.
     * Used by GeminiClient before building a prompt to check if context exists.
     */
    Optional<CourseAiContext> findByCourseId(Long courseId);

    /**
     * Check if a course already has context ingested.
     * Prevents redundant re-ingestion during startup batch.
     */
    boolean existsByCourseId(Long courseId);
}
