package com.edtech.lms.communication.services;

import com.edtech.lms.communication.models.dtos.ChatMessageRequest;
import com.edtech.lms.communication.models.dtos.ChatMessageResponse;
import com.edtech.lms.communication.models.entities.ChatMessageEntity;
import com.edtech.lms.communication.repositories.ChatMessageRepository;
import com.edtech.lms.communication.mappers.CommunicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling the core business logic for real-time chat interactions.
 * <p>
 * Responsible for securely encoding messages to Base64 before storing them in MongoDB,
 * and retrieving history for specific chat rooms partitioned by company and course.
 * </p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Saves a new chat message to MongoDB with Base64 encoding.
     * <p>
     * Encoding is necessary to safely persist emojis, special characters, and code snippets
     * without breaking the underlying BSON persistence layer.
     * </p>
     *
     * @param companyId the ID of the company this chat belongs to
     * @param courseId  the ID of the course this chat belongs to
     * @param request   the incoming request DTO containing the message payload
     * @return a ChatMessageResponse DTO representing the saved message
     */
    public ChatMessageResponse saveMessage(Long companyId, Long courseId, ChatMessageRequest request) {
        log.info("Saving chat message for company {} and course {}", companyId, courseId);
        
        String encodedContent = Base64.getEncoder().encodeToString(request.getContent().getBytes());
        
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .companyId(companyId)
                .courseId(courseId)
                .sender(request.getSender())
                .content(encodedContent)
                .timestamp(LocalDateTime.now())
                .build();
                
        ChatMessageEntity savedEntity = chatMessageRepository.save(entity);
        return CommunicationMapper.toChatMessageResponse(savedEntity);
    }

    /**
     * Retrieves the chronological chat history for a specific company and course room.
     *
     * @param companyId the ID of the company
     * @param courseId  the ID of the course
     * @return a List of ChatMessageResponse DTOs containing the decoded chat history
     */
    public List<ChatMessageResponse> getChatHistory(Long companyId, Long courseId) {
        log.info("Fetching chat history for company {} and course {}", companyId, courseId);
        
        List<ChatMessageEntity> entities = chatMessageRepository.findByCompanyIdAndCourseIdOrderByTimestampAsc(companyId, courseId);
        
        return entities.stream()
                .map(CommunicationMapper::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Scheduled job to delete chat messages older than 7 days.
     * Runs every day at 3:00 AM.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * ?")
    public void deleteOldMessages() {
        log.info("Starting scheduled job: Deleting chat messages older than 7 days");
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            chatMessageRepository.deleteByTimestampBefore(sevenDaysAgo);
            log.info("Successfully deleted chat messages older than {}", sevenDaysAgo);
        } catch (Exception e) {
            log.error("Error occurred while deleting old chat messages: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete old chat messages", e);
        }
    }
}
