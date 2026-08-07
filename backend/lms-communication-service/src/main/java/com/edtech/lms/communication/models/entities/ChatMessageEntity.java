package com.edtech.lms.communication.models.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessageEntity {

    @Id
    private String id;

    @Field("company_id")
    private Long companyId;

    @Field("course_id")
    private Long courseId;

    @Field("sender")
    private String sender;

    @Field("content")
    private String content; // Stored as Base64

    @Field("timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
