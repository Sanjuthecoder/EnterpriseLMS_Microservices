package com.edtech.lms.ai.kafka;

import com.edtech.lms.ai.client.GeminiClient;
import com.edtech.lms.ai.entity.CourseAiContext;
import com.edtech.lms.ai.repository.CourseAiContextRepository;
import com.edtech.lms.ai.service.AiQuizService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edtech.lms.ai.util.GeminiResponseParser;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseLifecycleFlowEndToEndTest {

    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Spy private GeminiResponseParser geminiResponseParser = new GeminiResponseParser(new ObjectMapper());
    @Mock private CourseAiContextRepository courseAiContextRepository;
    @Mock private GeminiClient geminiClient;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private CourseLifecycleConsumer consumer;
    private AiQuizService aiQuizService;

    @BeforeEach
    void setUp() {
        // Setup AiQuizService
        aiQuizService = new AiQuizService(geminiClient, courseAiContextRepository, geminiResponseParser, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(aiQuizService, "quizSystemInstruction", "SYS_PROMPT");
        ReflectionTestUtils.setField(aiQuizService, "preQuizPromptTemplate", "PRE_PROMPT");
        ReflectionTestUtils.setField(aiQuizService, "postQuizPromptTemplate", "POST_PROMPT");
        
        // Setup CourseLifecycleConsumer
        consumer = new CourseLifecycleConsumer(objectMapper, courseAiContextRepository, aiQuizService);
    }

    @Test
    @DisplayName("End-to-End: Consume course published -> Generate AI Quizzes -> Publish to Kafka")
    void endToEnd_consumeCoursePublished_generateAndPublishQuizzes() throws Exception {
        // 1. Mock DB Upsert logic to return a valid context
        CourseAiContext mockContext = new CourseAiContext();
        mockContext.setCourseId(100L);
        
        when(courseAiContextRepository.findByCourseId(100L)).thenReturn(Optional.of(mockContext));
        when(courseAiContextRepository.save(any(CourseAiContext.class))).thenAnswer(i -> i.getArgument(0));

        // 2. Mock Gemini API response (returning valid JSON chunk of quizzes)
        String mockGeminiResponse = """
                [
                  {
                    "courseId": 100,
                    "linkedLessonId": 10,
                    "quizType": "PRE_QUIZ",
                    "concept": "Java Streams",
                    "questionText": "What is a Stream?",
                    "correctAnswer": "A sequence of elements",
                    "options": [
                      "A sequence of elements",
                      "A byte array"
                    ],
                    "difficultyRating": "MEDIUM"
                  }
                ]
                """;
        when(geminiClient.generateText(anyString(), anyString())).thenReturn(mockGeminiResponse);

        // 3. Trigger the Kafka Listener method
        String incomingMessage = """
                {
                  "courseId": 100,
                  "title": "Advanced Java",
                  "description": "Learn advanced concepts",
                  "difficultyLevel": "ADVANCED",
                  "lessons": [
                    {
                      "lessonId": 10,
                      "title": "Java Streams",
                      "description": ""
                    }
                  ]
                }
                """;

        consumer.consumeCoursePublished(incomingMessage);

        // 4. Verify DB was queried and saved
        verify(courseAiContextRepository, times(2)).findByCourseId(100L);
        verify(courseAiContextRepository, atLeastOnce()).save(any(CourseAiContext.class));
        
        // 5. Verify Gemini API was called twice (Pre-quiz and Post-quiz chunks)
        verify(geminiClient, times(2)).generateText(anyString(), anyString());

        // 6. Verify the AI_QUIZ_GENERATED event was published back to Kafka
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        // Expect 1 send containing the combined PRE and POST quizzes
        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("ai-quiz-generated-topic");
        assertThat(keyCaptor.getValue()).isEqualTo("100");

        // Validate structure of the payload sent back to course-service
        String firstPayload = payloadCaptor.getValue();
        JsonNode payloadNode = objectMapper.readTree(firstPayload);
        
        assertThat(payloadNode.isObject()).isTrue();
        assertThat(payloadNode.get("courseId").asLong()).isEqualTo(100L);
        
        JsonNode questionsNode = payloadNode.get("questions");
        assertThat(questionsNode.isArray()).isTrue();
        // pre and post generation returned 1 question each
        assertThat(questionsNode.size()).isEqualTo(2);
        
        assertThat(questionsNode.get(0).get("quizType").asText()).isEqualTo("PRE_QUIZ");
        assertThat(questionsNode.get(0).get("questionText").asText()).isEqualTo("What is a Stream?");
        
        assertThat(questionsNode.get(1).get("quizType").asText()).isEqualTo("POST_QUIZ");
        assertThat(questionsNode.get(1).get("questionText").asText()).isEqualTo("What is a Stream?");
    }
}
