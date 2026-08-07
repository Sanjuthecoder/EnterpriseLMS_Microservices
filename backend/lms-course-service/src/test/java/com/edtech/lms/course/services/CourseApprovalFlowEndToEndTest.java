package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.Course;
import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.enums.CourseStatus;
import com.edtech.lms.course.repositories.CompanyCourseAvailabilityRepository;
import com.edtech.lms.course.repositories.CourseRepository;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import com.edtech.lms.course.repositories.EnrollmentRepository;
import com.edtech.lms.course.repositories.QuizQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseApprovalFlowEndToEndTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseStructureRepository courseStructureRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    
    @Mock private CompanyCourseAvailabilityRepository ccaRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private VideoInsightService videoInsightService;
    @Mock private QuizQuestionRepository quizQuestionRepository;

    private CreatorCourseService creatorCourseService;
    private SuperAdminCourseService superAdminCourseService;

    @BeforeEach
    void setUp() {
        creatorCourseService = new CreatorCourseService(
                courseRepository, courseStructureRepository, quizQuestionRepository, enrollmentRepository);
        
        superAdminCourseService = new SuperAdminCourseService(
                courseRepository, ccaRepository, enrollmentRepository, courseStructureRepository, 
                videoInsightService, kafkaTemplate, objectMapper);
                
        ReflectionTestUtils.setField(superAdminCourseService, "coursePublishedTopic", "course-published-topic");
    }

    @Test
    @DisplayName("End-to-End Flow: Creator uploads course -> Admin approves -> Kafka event emitted for AI Quizzes")
    void endToEnd_creatorUploads_adminApproves_kafkaEventEmitted() throws Exception {
        
        // =====================================================================
        // Phase 1: Creator creates and submits a course
        // =====================================================================
        
        Course newCourse = new Course();
        newCourse.setTitle("Advanced Java");
        newCourse.setDescription("Learn advanced concepts");
        newCourse.setDifficultyLevel(com.edtech.lms.course.models.enums.DifficultyLevel.ADVANCED);
        newCourse.setCreatorId("creator-1");
        
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            if (c.getCourseId() == null) {
                c.setCourseId(100L); // simulate DB auto-increment
            }
            return c;
        });

        // 1a. Create Course (Status becomes DRAFT)
        Course savedDraft = creatorCourseService.createCourse(newCourse);
        assertThat(savedDraft.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(savedDraft.getCourseId()).isEqualTo(100L);

        // 1b. Add a lesson
        CourseStructure lesson = new CourseStructure();
        lesson.setTitle("Java Streams");
        lesson.setLessonType(com.edtech.lms.course.models.enums.ContentType.VIDEO);
        
        when(courseRepository.findById(100L)).thenReturn(Optional.of(savedDraft));
        when(courseStructureRepository.save(any(CourseStructure.class))).thenAnswer(i -> {
            CourseStructure cs = i.getArgument(0);
            cs.setLessonId(10L);
            return cs;
        });
        
        creatorCourseService.addLesson(100L, lesson);
        
        // 1c. Submit for approval (Status becomes PENDING_APPROVAL)
        Course submittedCourse = creatorCourseService.submitForApproval(100L);
        assertThat(submittedCourse.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);

        // =====================================================================
        // Phase 2: Super Admin approves the course
        // =====================================================================
        
        // Mock the findById to return the submitted course
        when(courseRepository.findById(100L)).thenReturn(Optional.of(submittedCourse));
        // Mock the lessons lookup that the AI event payload builder needs
        when(courseStructureRepository.findByCourseIdOrderBySeqOrder(100L))
                .thenReturn(List.of(lesson));
                
        // 2a. Admin Approves (Status becomes PUBLISHED, Kafka event emitted)
        Course publishedCourse = superAdminCourseService.approveCourse(100L);
        
        assertThat(publishedCourse.getStatus()).isEqualTo(CourseStatus.PUBLISHED);

        // =====================================================================
        // Phase 3: Verify AI Quiz Kafka Event
        // =====================================================================
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
        
        assertThat(topicCaptor.getValue()).isEqualTo("course-published-topic");
        assertThat(keyCaptor.getValue()).isEqualTo("100");
        
        // Verify JSON payload structure for the AI Service
        String payloadJson = payloadCaptor.getValue();
        JsonNode rootNode = objectMapper.readTree(payloadJson);
        
        assertThat(rootNode.get("courseId").asLong()).isEqualTo(100L);
        assertThat(rootNode.get("title").asText()).isEqualTo("Advanced Java");
        assertThat(rootNode.get("description").asText()).isEqualTo("Learn advanced concepts");
        assertThat(rootNode.get("difficultyLevel").asText()).isEqualTo("ADVANCED");
        
        JsonNode lessonsNode = rootNode.get("lessons");
        assertThat(lessonsNode.isArray()).isTrue();
        assertThat(lessonsNode.size()).isEqualTo(1);
        assertThat(lessonsNode.get(0).asText()).contains("Java Streams");
    }
}
