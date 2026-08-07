package com.edtech.lms.communication.controllers;

import com.edtech.lms.communication.models.dtos.ChatMessageRequest;
import com.edtech.lms.communication.models.dtos.ChatMessageResponse;
import com.edtech.lms.communication.services.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {

    private final ChatService chatService;

    /**
     * Receives messages from clients sent to /app/chat.send/{companyId}/{courseId}
     * and broadcasts to all subscribers of /topic/company/{companyId}/course/{courseId}.
     *
     * @param companyId the ID of the company
     * @param courseId  the ID of the course
     * @param request   the chat message payload
     * @return the saved chat message response
     */
    @MessageMapping("/chat.send/{companyId}/{courseId}")
    @SendTo("/topic/company/{companyId}/course/{courseId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable Long companyId,
            @DestinationVariable Long courseId,
            @Payload ChatMessageRequest request) {
        
        log.debug("Received websocket message for company {} and course {}: {}", companyId, courseId, request.getContent());
        
        // Save to DB and encode before broadcasting
        return chatService.saveMessage(companyId, courseId, request);
    }

    /**
     * REST endpoint to fetch chat history for a specific company and course.
     *
     * @param companyId the ID of the company
     * @param courseId  the ID of the course
     * @return a list of chat message responses representing the history
     */
    @GetMapping("/history/{companyId}/{courseId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long companyId,
            @PathVariable Long courseId) {
            
        log.debug("REST request to fetch chat history for company {} and course {}", companyId, courseId);
        List<ChatMessageResponse> history = chatService.getChatHistory(companyId, courseId);
        return ResponseEntity.ok(history);
    }
}
