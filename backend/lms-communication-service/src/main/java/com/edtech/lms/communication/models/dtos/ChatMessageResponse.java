package com.edtech.lms.communication.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private Long companyId;
    private Long courseId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
}
