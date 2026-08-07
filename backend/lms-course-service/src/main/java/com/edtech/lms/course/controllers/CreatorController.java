package com.edtech.lms.course.controllers;

import com.edtech.lms.course.models.dtos.CourseResponse;
import com.edtech.lms.course.mappers.CourseMapper;
import com.edtech.lms.course.exceptions.UnauthorizedException;
import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.entities.QuizQuestion;
import com.edtech.lms.course.services.CreatorCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * CreatorController — REST API for content creator operations.
 *
 * Routes: /api/creator/**
 * Multi-tenancy: creatorId is extracted from X-User-Id header (injected by Gateway).
 * Security: Only CREATOR role can access these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorCourseService creatorCourseService;

    // =========================================================================
    // COURSE MANAGEMENT
    // =========================================================================

    /**
     * GET /api/creator/courses
     * Returns all courses authored by the calling creator.
     */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getAuthoredCourses(
            @RequestHeader(value = "X-User-Id", required = false) String creatorId,
            @RequestParam(required = false) String creatorId_param) {
        String id = creatorId != null ? creatorId : creatorId_param;
        if (id == null || id.isBlank()) {
            throw new UnauthorizedException("Missing creator identification");
        }
        
        List<CourseResponse> courses = creatorCourseService.getCreatorCourses(id)
                .stream()
                .map(CourseMapper::toCourseResponse)
                .toList();
                
        return ResponseEntity.ok(courses);
    }

    /**
     * POST /api/creator/courses
     * Creates a new course draft.
     */
    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(
            @RequestHeader(value = "X-User-Id", required = false) String creatorId,
            @RequestBody Course course) {
        if (creatorId != null && !creatorId.isBlank()) {
            course.setCreatorId(creatorId);
        }
        Course saved = creatorCourseService.createCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseMapper.toCourseResponse(saved));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @RequestBody Course updates) {
        Course updated = creatorCourseService.updateCourse(id, updates);
        return ResponseEntity.ok(CourseMapper.toCourseResponse(updated));
    }

    @PostMapping("/courses/{id}/submit")
    public ResponseEntity<CourseResponse> submitForApproval(@PathVariable Long id) {
        Course submitted = creatorCourseService.submitForApproval(id);
        return ResponseEntity.ok(CourseMapper.toCourseResponse(submitted));
    }

    // =========================================================================
    // LESSON MANAGEMENT
    // =========================================================================

    /**
     * GET /api/creator/courses/{id}/lessons
     * Returns ordered list of lessons for the course.
     */
    @GetMapping("/courses/{id}/lessons")
    public ResponseEntity<List<CourseStructure>> getLessons(@PathVariable Long id) {
        return ResponseEntity.ok(creatorCourseService.getLessons(id));
    }

    /**
     * POST /api/creator/courses/{id}/lessons
     * Adds a new lesson to the course.
     */
    @PostMapping("/courses/{id}/lessons")
    public ResponseEntity<CourseStructure> addLesson(
            @PathVariable Long id,
            @RequestBody CourseStructure lesson) {
        CourseStructure saved = creatorCourseService.addLesson(id, lesson);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // =========================================================================
    // QUIZ MANAGEMENT
    // =========================================================================

    /**
     * GET /api/creator/courses/{id}/questions
     * Returns all quiz questions for the course (both PRE and POST quiz).
     */
    @GetMapping("/courses/{id}/questions")
    public ResponseEntity<List<QuizQuestion>> getQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(creatorCourseService.getQuestions(id));
    }

    /**
     * POST /api/creator/courses/{id}/questions
     * Adds a quiz question to the course.
     */
    @PostMapping("/courses/{id}/questions")
    public ResponseEntity<QuizQuestion> addQuestion(
            @PathVariable Long id,
            @RequestBody QuizQuestion question) {
        QuizQuestion saved = creatorCourseService.addQuestion(id, question);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // =========================================================================
    // MEDIA UPLOAD
    // =========================================================================

    /**
     * POST /api/creator/media/upload
     * Uploads a media file and returns its accessible URL.
     */
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMedia(@RequestParam("file") MultipartFile file) throws Exception {
        String url = creatorCourseService.uploadMedia(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // =========================================================================
    // DASHBOARD STATS
    // =========================================================================

    /**
     * GET /api/creator/stats
     * Returns active learner count and video lesson count for the creator dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCreatorStats(
            @RequestHeader(value = "X-User-Id", required = false) String creatorId,
            @RequestParam(required = false) String creatorId_param) {
        String id = creatorId != null ? creatorId : creatorId_param;
        if (id == null || id.isBlank()) {
            throw new UnauthorizedException("Missing creator identification");
        }
        return ResponseEntity.ok(creatorCourseService.getCreatorStats(id));
    }

}
