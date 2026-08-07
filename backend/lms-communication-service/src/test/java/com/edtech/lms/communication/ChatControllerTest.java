package com.edtech.lms.communication;

import com.edtech.lms.communication.controllers.ChatController;
import com.edtech.lms.communication.models.dtos.ChatMessageRequest;
import com.edtech.lms.communication.models.dtos.ChatMessageResponse;
import com.edtech.lms.communication.services.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ChatControllerTest {

    @Autowired
    private ChatController chatController;

    @MockBean
    private ChatService chatService;

    @Test
    public void testSendMessage() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .sender("User A")
                .content("Hello everyone!")
                .build();
                
        ChatMessageResponse mockResponse = ChatMessageResponse.builder()
                .companyId(1L)
                .courseId(100L)
                .sender("User A")
                .content("Hello everyone!")
                .timestamp(LocalDateTime.now())
                .build();
                
        when(chatService.saveMessage(eq(1L), eq(100L), any(ChatMessageRequest.class))).thenReturn(mockResponse);

        ChatMessageResponse response = chatController.sendMessage(1L, 100L, request);

        assertNotNull(response);
        assertEquals(100L, response.getCourseId());
        assertEquals(1L, response.getCompanyId());
        assertEquals("Hello everyone!", response.getContent());
    }
    
    @Test
    public void testGetChatHistory() {
        ChatMessageResponse mockResponse = ChatMessageResponse.builder()
                .companyId(1L)
                .courseId(100L)
                .sender("User A")
                .content("History Message")
                .timestamp(LocalDateTime.now())
                .build();
                
        when(chatService.getChatHistory(1L, 100L)).thenReturn(List.of(mockResponse));
        
        ResponseEntity<List<ChatMessageResponse>> response = chatController.getChatHistory(1L, 100L);
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("History Message", response.getBody().get(0).getContent());
    }
}
