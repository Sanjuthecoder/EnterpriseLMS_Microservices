package com.edtech.lms.communication.mappers;

import com.edtech.lms.communication.models.dtos.ChatMessageResponse;
import com.edtech.lms.communication.models.entities.ChatMessageEntity;

import java.util.Base64;

/**
 * Mapper utility for Communication Service.
 * <p>
 * This class handles mapping between internal database entities and public-facing DTOs.
 * Specifically, it handles the Base64 decoding of chat messages before they are returned to the client.
 * </p>
 */
public class CommunicationMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CommunicationMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts a ChatMessageEntity to a ChatMessageResponse DTO.
     * Decodes the Base64 encoded content string.
     *
     * @param entity the ChatMessageEntity retrieved from the database
     * @return a mapped ChatMessageResponse DTO
     */
    public static ChatMessageResponse toChatMessageResponse(ChatMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        String decodedContent = new String(Base64.getDecoder().decode(entity.getContent()));

        return ChatMessageResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .courseId(entity.getCourseId())
                .sender(entity.getSender())
                .content(decodedContent)
                .timestamp(entity.getTimestamp())
                .build();
    }
}
