package com.edtech.lms.ai.constant;

/**
 * AiConstants — All magic-free string/numeric constants for lms-ai-service.
 *
 * Usage: Reference these from service and scheduler classes only.
 * Never use raw string literals in business logic.
 */
public final class AiConstants {

    private AiConstants() {
        // Utility class — prevent instantiation
    }

    // =========================================================================
    // KAFKA TOPICS
    // =========================================================================

    /** Topic produced by lms-course-service when a new course is published/updated */
    public static final String TOPIC_COURSE_LIFECYCLE    = "course-lifecycle-topic";

    /** Topic consumed by lms-ai-service: raw xAPI quiz data from lms-telemetry-service */
    public static final String TOPIC_XAPI_STATEMENTS     = "xapi-statements-topic";

    /**
     * Topic produced by lms-ai-service with AI-generated quiz questions.
     * Consumed by lms-course-service AiQuizKafkaConsumer to persist to MySQL.
     */
    public static final String TOPIC_AI_QUIZ_GENERATED   = "ai-quiz-generated-topic";

    /**
     * Max lessons per Gemini prompt chunk to avoid token-limit degradation on large courses.
     */
    public static final int LESSON_CHUNK_SIZE = 8;


    // =========================================================================
    // MONGODB COLLECTION NAMES
    // =========================================================================

    /** Stores AI-generated insight reports for Company Admins and Super Admins */
    public static final String COLLECTION_AI_INSIGHTS         = "svc_ai_insights";

    /** Stores course content snapshots fetched for AI context (not embeddings — text chunks) */
    public static final String COLLECTION_AI_COURSE_CONTEXT   = "svc_ai_course_context";

    /** Retry limit for failed Gemini API calls before throwing AiProviderUnavailableException */
    public static final int MAX_AI_RETRY_COUNT = 3;

}
