package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.entities.Enrollment;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.models.enums.ContentType;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.models.enums.EnrollmentStatus;
import com.edtech.lms.course.models.enums.QuizType;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmployeeCourseService — All employee-facing course operations.
 *
 * Multi-tenancy: All methods scope data access to (employeeId, companyId).
 * Responsibilities:
 * - Dashboard: enrolled courses with progress
 * - Pre-Quiz retrieval and gating submission
 * - Course content with gating badges
 * - Lesson completion and progress tracking
 * - Post-Quiz retrieval and uplift calculation
 *
 * NOTE: Video telemetry is handled by lms-telemetry-service.
 * This service calls telemetry for the lesson completion signal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeCourseService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseStructureRepository courseStructureRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final org.springframework.web.client.RestTemplate restTemplate;
    
    @org.springframework.beans.factory.annotation.Value("${app.kafka.topics.xapi-statements:xapi-statements-topic}")
    private String xapiTopic;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    /**
     * Returns all enrolled courses for the employee with progress and gating info.
     */
    public List<Map<String, Object>> getEmployeeDashboard(String employeeId, String companyId) {
        List<Enrollment> enrollments = enrollmentRepository.findByEmployeeIdAndCompanyId(employeeId, companyId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Enrollment e : enrollments) {
            Map<String, Object> row = new HashMap<>();
            row.put("enrollmentId", e.getEnrollmentId());
            row.put("courseId", e.getCourseId());
            row.put("status", e.getStatus().name());
            row.put("progressPercentage", e.getProgressPercentage());
            row.put("deadline", e.getDeadline());
            row.put("assignedDate", e.getAssignedDate());
            row.put("preQuizScore", e.getPreQuizScore());
            row.put("postQuizScore", e.getPostQuizScore());
            row.put("upliftPercent", e.getUpliftPercent());
            row.put("lessonGatingMap", e.getLessonGatingMap());
            row.put("certificateStatus", e.getCertificateStatus());
            courseRepository.findById(e.getCourseId()).ifPresent(c -> {
                row.put("courseTitle", c.getTitle());
                row.put("courseDescription", c.getDescription());
                row.put("difficultyLevel", c.getDifficultyLevel() != null ? c.getDifficultyLevel().name() : null);
            });
            result.add(row);
        }
        return result;
    }

    // =========================================================================
    // PRE-QUIZ
    // =========================================================================

    /**
     * Returns pre-quiz questions for a course using tier-based smart routing.
     *
     * <p><b>Routing logic:</b>
     * <ul>
     *   <li>Premium: tries AI questions first; falls back to creator questions if AI is still pending.</li>
     *   <li>Non-Premium: always returns creator-authored questions.</li>
     * </ul>
     *
     * <p><b>Race condition guard:</b> The enrollment's {@code quizSessionSource} is set on first
     * access so that both PRE and POST quizzes always come from the same source, even if AI finishes
     * generating between the two quiz events.
     *
     * @param employeeId the enrolled employee
     * @param companyId  the company for multi-tenancy and tier lookup
     * @param courseId   the course being assessed
     * @return sanitized list of quiz questions (correctAnswer excluded)
     */
    public List<Map<String, Object>> getPreQuizQuestions(String employeeId, String companyId, Long courseId, String subscriptionTier) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);

        if (enrollment.getLessonGatingMap() != null) {
            throw new IllegalStateException("Pre-quiz already completed for this course.");
        }

        List<QuizQuestion> questions = resolveQuizQuestions(courseId, QuizType.PRE_QUIZ, subscriptionTier);
        return sanitizeQuestions(questions);
    }

    /**
     * Submits pre-quiz answers. Applies the 3-factor gating engine.
     * Writes lessonGatingMap and preQuizScore to the Enrollment.
     *
     * Gating engine: For each question answered, if ANY of these fires → lesson = RECOMMENDED:
     *   1. Incorrect answer
     *   2. Answer changes > 2 (high hesitation)
     *   3. Time spent > 15000ms (cognitive overload)
     * Otherwise → OPTIONAL
     */
    @Transactional
    public Map<String, Object> submitPreQuiz(String employeeId, String companyId, Long courseId, List<Map<String, Object>> answers) {
        log.info("Processing pre-quiz submission: employee={}, course={}", employeeId, courseId);
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);

        if (enrollment.getLessonGatingMap() != null) {
            throw new IllegalStateException("Pre-quiz already submitted.");
        }

        List<QuizQuestion> questions = quizQuestionRepository.findByCourseIdAndQuizType(courseId, QuizType.PRE_QUIZ);
        Map<Long, QuizQuestion> questionMap = new HashMap<>();
        questions.forEach(q -> questionMap.put(q.getQuestionId(), q));

        // Build per-lesson gating decisions
        Map<String, String> lessonGating = new HashMap<>();
        Map<String, String> correctAnswers = new HashMap<>();
        int correct = 0;

        for (Map<String, Object> answer : answers) {
            Long questionId = Long.parseLong(answer.get("questionId").toString());
            String submitted = answer.get("answer") != null ? answer.get("answer").toString() : "";
            int answerChanges = answer.containsKey("answerChanges") ? Integer.parseInt(answer.get("answerChanges").toString()) : 0;
            long timeSpentMs = answer.containsKey("timeSpentMs") ? Long.parseLong(answer.get("timeSpentMs").toString()) : 0L;

            QuizQuestion question = questionMap.get(questionId);
            if (question == null) continue;

            boolean isCorrect = false;
            String correctAnswerDb = question.getCorrectAnswer();
            String correctIndexForFrontend = correctAnswerDb;

            if (Boolean.TRUE.equals(question.getIsAiGenerated())) {
                try {
                    int submittedIndex = Integer.parseInt(submitted.trim());
                    com.fasterxml.jackson.databind.JsonNode optionsNode = question.getOptions();
                    if (optionsNode != null && optionsNode.isArray() && submittedIndex >= 0 && submittedIndex < optionsNode.size()) {
                        String selectedText = optionsNode.get(submittedIndex).asText();
                        isCorrect = correctAnswerDb.equalsIgnoreCase(selectedText.trim());
                    }
                    // Determine index of correct answer for frontend highlighting
                    if (optionsNode != null && optionsNode.isArray()) {
                        for (int i = 0; i < optionsNode.size(); i++) {
                            if (optionsNode.get(i).asText().equalsIgnoreCase(correctAnswerDb)) {
                                correctIndexForFrontend = String.valueOf(i);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse options or index for AI question {}", questionId, e);
                }
            } else {
                isCorrect = correctAnswerDb.equalsIgnoreCase(submitted.trim());
            }

            if (isCorrect) correct++;
            correctAnswers.put(String.valueOf(questionId), correctIndexForFrontend);

            boolean highHesitation = answerChanges > 2;
            boolean cognitiveOverload = timeSpentMs > 15000;

            // 3-factor gating: ANY factor fires → RECOMMENDED
            boolean isRecommended = !isCorrect || highHesitation || cognitiveOverload;
            String lessonIdKey = question.getLessonId() != null ? String.valueOf(question.getLessonId()) : String.valueOf(questionId);
            lessonGating.put(lessonIdKey, isRecommended ? "RECOMMENDED" : "OPTIONAL");

            String triggerReason = null;
            if (!isCorrect) {
                triggerReason = submitted.isEmpty() ? "Skipped/Unanswered" : "Incorrect Response";
            } else if (highHesitation) {
                triggerReason = "High Hesitation (>2 Flips)";
            } else if (cognitiveOverload) {
                triggerReason = "High Cognitive Load (>15s)";
            }

            saveQuizXapiStatement(Long.parseLong(employeeId), courseId, Long.parseLong(companyId), 
                    question, submitted, isCorrect, timeSpentMs, answerChanges, triggerReason, "PRE_QUIZ");
        }

        double preQuizScore = questions.isEmpty() ? 0.0 : (correct * 100.0 / questions.size());

        // Persist gating map and score
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        enrollment.setLessonGatingMap(mapper.valueToTree(lessonGating));
        enrollment.setPreQuizScore(preQuizScore);
        if (enrollment.getStatus() == EnrollmentStatus.ASSIGNED) {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        }
        enrollmentRepository.save(enrollment);

        Map<String, Object> response = new HashMap<>();
        response.put("preQuizScore", preQuizScore);
        response.put("lessonGatingMap", lessonGating);
        response.put("correctAnswers", correctAnswers);
        response.put("message", "Pre-quiz submitted. Your personalized learning path has been generated.");
        return response;
    }
    
    public void updateAiGating(String employeeId, String companyId, Long courseId, Map<String, String> lessonGating, double preQuizScore) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        enrollment.setLessonGatingMap(mapper.valueToTree(lessonGating));
        enrollment.setPreQuizScore(preQuizScore);
        if (enrollment.getStatus() == EnrollmentStatus.ASSIGNED) {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        }
        enrollmentRepository.save(enrollment);
    }

    // =========================================================================
    // COURSE CONTENT
    // =========================================================================

    /**
     * Returns ordered lessons with RECOMMENDED/OPTIONAL gating tags.
     * Null gating = pre-quiz not yet taken.
     */
    public Map<String, Object> getCourseContent(String employeeId, String companyId, Long courseId) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<CourseStructure> lessons = courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId);

        // Build gating lookup
        Map<String, String> gatingMap = new HashMap<>();
        if (enrollment.getLessonGatingMap() != null) {
            enrollment.getLessonGatingMap().fields()
                    .forEachRemaining(entry -> gatingMap.put(entry.getKey(), entry.getValue().asText()));
        }

        List<Map<String, Object>> content = lessons.stream().map(lesson -> {
            Map<String, Object> row = new HashMap<>();
            row.put("lessonId", lesson.getLessonId());
            row.put("seqOrder", lesson.getSeqOrder());
            row.put("lessonTitle", lesson.getTitle());
            row.put("moduleTitle", lesson.getModuleTitle());
            row.put("contentType", lesson.getLessonType() != null ? lesson.getLessonType().name() : "VIDEO");
            row.put("contentUrl", lesson.getContentUrl());
            row.put("durationMinutes", lesson.getDurationMinutes());
            row.put("gatingStatus", gatingMap.getOrDefault(String.valueOf(lesson.getLessonId()), null));
            return row;
        }).toList();

        List<Long> completedIds = new ArrayList<>();
        if (enrollment.getCompletedLessons() != null && enrollment.getCompletedLessons().isArray()) {
            enrollment.getCompletedLessons().forEach(n -> completedIds.add(n.asLong()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("courseTitle", course.getTitle());
        response.put("lessons", content);
        response.put("completedLessons", completedIds);
        response.put("progressPercentage", enrollment.getProgressPercentage() != null ? enrollment.getProgressPercentage() : 0);
        return response;
    }

    // =========================================================================
    // LESSON COMPLETION
    // =========================================================================

    /**
     * Marks a lesson as completed and recalculates progress.
     * Progress is calculated only on RECOMMENDED lessons (not optional ones).
     */
    @Transactional
    public Map<String, Object> markLessonComplete(String employeeId, String companyId, Long courseId, Long lessonId) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);

        List<Long> completedIds = new ArrayList<>();
        if (enrollment.getCompletedLessons() != null && enrollment.getCompletedLessons().isArray()) {
            enrollment.getCompletedLessons().forEach(n -> completedIds.add(n.asLong()));
        }

        if (!completedIds.contains(lessonId)) {
            completedIds.add(lessonId);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
            completedIds.forEach(arr::add);
            enrollment.setCompletedLessons(arr);
        }

        List<CourseStructure> lessons = courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId);
        int progress = calculateProgress(enrollment.getLessonGatingMap(), lessons, completedIds);
        enrollment.setProgressPercentage(progress);

        if (enrollment.getStatus() == EnrollmentStatus.ASSIGNED) {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        }
        enrollmentRepository.save(enrollment);

        return Map.of("completedLessons", completedIds, "progressPercentage", progress);
    }

    // =========================================================================
    // POST-QUIZ
    // =========================================================================

    /**
     * Returns post-quiz questions using tier-based smart routing (same logic as pre-quiz).
     *
     * @param employeeId the enrolled employee
     * @param companyId  company context for tier lookup
     * @param courseId   the course being assessed
     * @return sanitized post-quiz questions
     */
    public List<Map<String, Object>> getPostQuizQuestions(String employeeId, String companyId, Long courseId, String subscriptionTier) {
        getEnrollmentOrThrow(employeeId, courseId, companyId);
        List<QuizQuestion> questions = resolveQuizQuestions(courseId, QuizType.POST_QUIZ, subscriptionTier);
        return sanitizeQuestions(questions);
    }

    /**
     * Submits post-quiz. Computes uplift delta vs pre-quiz score.
     * Writes postQuizScore, upliftPercent, and upliftReport to enrollment.
     */
    @Transactional
    public Map<String, Object> submitPostQuiz(String employeeId, String companyId, Long courseId, String subscriptionTier, List<Map<String, Object>> answers) {
        log.info("Processing post-quiz: employee={}, course={}", employeeId, courseId);
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);

        List<QuizQuestion> questions = resolveQuizQuestions(courseId, QuizType.POST_QUIZ, subscriptionTier);
        Map<Long, QuizQuestion> questionMap = new HashMap<>();
        questions.forEach(q -> questionMap.put(q.getQuestionId(), q));

        int correct = 0;
        Map<String, Boolean> preResults = new HashMap<>();
        Map<String, Boolean> postResults = new HashMap<>();
        Map<String, String> correctAnswers = new HashMap<>();

        for (Map<String, Object> answer : answers) {
            Long questionId = Long.parseLong(answer.get("questionId").toString());
            String submitted = answer.get("answer") != null ? answer.get("answer").toString() : "";
            QuizQuestion question = questionMap.get(questionId);
            if (question == null) continue;

            boolean isCorrect = false;
            String correctAnswerDb = question.getCorrectAnswer();
            String correctIndexForFrontend = correctAnswerDb;

            if (Boolean.TRUE.equals(question.getIsAiGenerated())) {
                try {
                    int submittedIndex = Integer.parseInt(submitted.trim());
                    com.fasterxml.jackson.databind.JsonNode optionsNode = question.getOptions();
                    if (optionsNode != null && optionsNode.isArray() && submittedIndex >= 0 && submittedIndex < optionsNode.size()) {
                        String selectedText = optionsNode.get(submittedIndex).asText();
                        isCorrect = correctAnswerDb.equalsIgnoreCase(selectedText.trim());
                    }
                    if (optionsNode != null && optionsNode.isArray()) {
                        for (int i = 0; i < optionsNode.size(); i++) {
                            if (optionsNode.get(i).asText().equalsIgnoreCase(correctAnswerDb)) {
                                correctIndexForFrontend = String.valueOf(i);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse options or index for AI question {}", questionId, e);
                }
            } else {
                isCorrect = correctAnswerDb.equalsIgnoreCase(submitted.trim());
            }

            if (isCorrect) correct++;
            correctAnswers.put(String.valueOf(questionId), correctIndexForFrontend);
            postResults.put(question.getConcept(), isCorrect);

            saveQuizXapiStatement(Long.parseLong(employeeId), courseId, Long.parseLong(companyId), 
                    question, submitted, isCorrect, null, null, null, "POST_QUIZ");
        }

        double postScore = questions.isEmpty() ? 0.0 : (correct * 100.0 / questions.size());
        double preScore = enrollment.getPreQuizScore() != null ? enrollment.getPreQuizScore() : 0.0;
        double uplift = postScore - preScore;

        // Build uplift report
        List<String> gained = new ArrayList<>();
        List<String> struggling = new ArrayList<>();
        postResults.forEach((concept, passed) -> {
            if (passed) gained.add(concept);
            else struggling.add(concept);
        });

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> upliftReport = Map.of(
                "conceptsGained", gained,
                "stillStruggling", struggling
        );

        enrollment.setPostQuizScore(postScore);
        enrollment.setUpliftPercent(uplift);
        enrollment.setUpliftReport(mapper.valueToTree(upliftReport));
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletionDate(java.time.LocalDateTime.now());
        enrollment.setCertificateStatus("ELIGIBLE"); // Enable certificate request
        enrollmentRepository.save(enrollment);

        Map<String, Object> response = new HashMap<>();
        response.put("postQuizScore", postScore);
        response.put("preQuizScore", preScore);
        response.put("upliftPercent", uplift);
        response.put("upliftReport", upliftReport);
        response.put("correctAnswers", correctAnswers);
        return response;
    }

    // =========================================================================
    // CERTIFICATES
    // =========================================================================

    public Map<String, Object> getUpliftReport(String employeeId, String companyId, Long courseId) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);
        Map<String, Object> result = new HashMap<>();
        result.put("preQuizScore", enrollment.getPreQuizScore());
        result.put("postQuizScore", enrollment.getPostQuizScore());
        result.put("upliftPercent", enrollment.getUpliftPercent());
        result.put("upliftReport", enrollment.getUpliftReport());
        courseRepository.findById(courseId).ifPresent(c -> result.put("courseTitle", c.getTitle()));
        return result;
    }

    @Transactional
    public Map<String, Object> requestCertificate(String employeeId, String companyId, Long courseId) {
        Enrollment enrollment = getEnrollmentOrThrow(employeeId, courseId, companyId);
        if (!"ELIGIBLE".equals(enrollment.getCertificateStatus())) {
            throw new IllegalStateException("Cannot request certificate. Current status: " + enrollment.getCertificateStatus());
        }
        enrollment.setCertificateStatus("REQUESTED");
        enrollmentRepository.save(enrollment);
        return Map.of("message", "Certificate request submitted successfully", "certificateStatus", "REQUESTED");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    // =========================================================================
    // SMART QUIZ ROUTING
    // =========================================================================

    /**
     * Resolves the correct set of quiz questions based on the company's subscription tier.
     *
     * <p><b>Premium routing:</b>
     * <ol>
     *   <li>Query for AI-generated ({@code isAiGenerated = true}) questions.</li>
     *   <li>If none exist (AI still generating), fall back to creator questions.</li>
     * </ol>
     *
     * <p><b>Non-Premium routing:</b> always return creator-authored questions only.
     *
     * @param courseId  the course identifier
     * @param quizType  PRE_QUIZ or POST_QUIZ
     * @param companyId used to determine the subscription tier
     * @return the appropriate list of questions; never null
     */
    private List<QuizQuestion> resolveQuizQuestions(
            final Long courseId, final QuizType quizType, final String subscriptionTier) {

        if (isPremiumTier(subscriptionTier)) {
            List<QuizQuestion> aiQuestions = quizQuestionRepository
                    .findByCourseIdAndQuizTypeAndIsAiGenerated(courseId, quizType, true);

            if (!aiQuestions.isEmpty()) {
                log.debug("Serving AI questions for courseId={}, quizType={}", courseId, quizType);
                return aiQuestions;
            }

            // AI generation is still PENDING — fall back to creator questions gracefully.
            // This satisfies the race-condition requirement: the user is not blocked.
            log.info("AI questions not yet ready for courseId={}, falling back to creator questions.", courseId);
        }

        return quizQuestionRepository
                .findByCourseIdAndQuizTypeAndIsAiGenerated(courseId, quizType, false);
    }

    /**
     * Determines whether a user's subscription tier is Premium.
     *
     * @param subscriptionTier the subscription tier from the JWT token
     * @return {@code true} if the tier is Premium
     */
    private boolean isPremiumTier(final String subscriptionTier) {
        return "PREMIUM".equalsIgnoreCase(subscriptionTier);
    }

    private Enrollment getEnrollmentOrThrow(String employeeId, Long courseId, String companyId) {
        return enrollmentRepository.findByEmployeeIdAndCourseIdAndCompanyId(employeeId, courseId, companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enrollment not found for employee=" + employeeId + " course=" + courseId));
    }

    private List<Map<String, Object>> sanitizeQuestions(List<QuizQuestion> questions) {
        return questions.stream().map(q -> {
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", q.getQuestionId());
            row.put("concept", q.getConcept());
            row.put("questionText", q.getQuestionText());
            row.put("options", q.getOptions());
            row.put("linkedLessonId", q.getLessonId());
            // Correct answer intentionally excluded
            return row;
        }).toList();
    }

    private int calculateProgress(JsonNode gatingMap, List<CourseStructure> lessons, List<Long> completedIds) {
        long requiredCount = 0;
        long completedRequired = 0;

        for (CourseStructure lesson : lessons) {
            boolean isRequired = true;
            if (gatingMap != null && gatingMap.has(String.valueOf(lesson.getLessonId()))) {
                String status = gatingMap.get(String.valueOf(lesson.getLessonId())).asText();
                if ("OPTIONAL".equalsIgnoreCase(status)) isRequired = false;
            }
            if (isRequired) {
                requiredCount++;
                if (completedIds.contains(lesson.getLessonId())) completedRequired++;
            }
        }

        if (requiredCount == 0) return 100;
        return (int) Math.min(Math.round((completedRequired * 100.0) / requiredCount), 100);
    }

    private void saveQuizXapiStatement(Long employeeId, Long courseId, Long companyId, QuizQuestion q,
                                       String finalAnswer, boolean isCorrect, Long timeSpentMs, Integer answerChanges, 
                                       String triggerReason, String quizType) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode actor = objectMapper.createObjectNode();
            actor.put("mbox", "mailto:employee" + employeeId + "@company" + companyId + ".com");
            actor.put("name", "Employee " + employeeId);

            com.fasterxml.jackson.databind.node.ObjectNode verb = objectMapper.createObjectNode();
            verb.put("id", "http://adlnet.gov/expapi/verbs/answered");
            verb.put("display", "answered");

            com.fasterxml.jackson.databind.node.ObjectNode objDef = objectMapper.createObjectNode();
            objDef.put("name", q.getQuestionText());
            objDef.put("type", "question");

            com.fasterxml.jackson.databind.node.ObjectNode object = objectMapper.createObjectNode();
            object.put("id", "course/" + courseId + "/quiz/" + q.getQuestionId());
            object.set("definition", objDef);

            com.fasterxml.jackson.databind.node.ObjectNode score = objectMapper.createObjectNode();
            score.put("scaled", isCorrect ? 1.0 : 0.0);
            score.put("raw", isCorrect ? 1 : 0);
            score.put("min", 0);
            score.put("max", 1);

            com.fasterxml.jackson.databind.node.ObjectNode result = objectMapper.createObjectNode();
            result.put("success", isCorrect);
            result.put("response", finalAnswer != null ? finalAnswer : "");
            result.set("score", score);

            com.fasterxml.jackson.databind.node.ObjectNode extensions = objectMapper.createObjectNode();
            extensions.put("concept", q.getConcept());
            if (q.getLessonId() != null) extensions.put("linked_lesson_id", q.getLessonId());
            if (timeSpentMs != null)  extensions.put("time_spent_ms", timeSpentMs);
            if (answerChanges != null) extensions.put("answer_changes", answerChanges);
            if (triggerReason != null) extensions.put("trigger_reason", triggerReason);
            extensions.put("quiz_type", quizType);
            // xAPI telemetry granularity: lets the data team differentiate AI vs. creator question performance
            extensions.put("is_ai_generated", q.getIsAiGenerated() != null && q.getIsAiGenerated());

            com.fasterxml.jackson.databind.node.ObjectNode context = objectMapper.createObjectNode();
            context.set("extensions", extensions);

            com.fasterxml.jackson.databind.node.ObjectNode xapiNode = objectMapper.createObjectNode();
            xapiNode.put("orgId", 0); // Defaulting to 0 since we don't have orgId in this service context
            xapiNode.put("companyId", companyId);
            xapiNode.put("employeeId", employeeId);
            xapiNode.put("courseId", courseId);
            xapiNode.set("actor", actor);
            xapiNode.set("verb", verb);
            xapiNode.set("object", object);
            xapiNode.set("result", result);
            xapiNode.set("context", context);
            xapiNode.put("timestamp", java.time.LocalDateTime.now().toString());

            // Temporary REST fallback since Kafka is not installed in the dev environment
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(objectMapper.writeValueAsString(xapiNode), headers);
                restTemplate.postForEntity("http://lms-telemetry-service/api/telemetry/xapi-statements", request, String.class);
            } catch (Exception e) {
                log.warn("REST telemetry fallback failed, trying Kafka: {}", e.getMessage());
                kafkaTemplate.send(xapiTopic, employeeId.toString(), objectMapper.writeValueAsString(xapiNode));
            }
        } catch (Exception e) {
            log.error("Failed to send XAPI statement event: {}", e.getMessage(), e);
        }
    }
}
