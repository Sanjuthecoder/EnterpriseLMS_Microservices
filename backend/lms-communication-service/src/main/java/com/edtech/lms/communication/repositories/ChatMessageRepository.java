package com.edtech.lms.communication.repositories;

import com.edtech.lms.communication.models.entities.ChatMessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessageEntity, String> {
    
    /**
     * Finds chat messages for a specific company and course.
     *
     * @param companyId company identifier
     * @param courseId course identifier
     * @return list of chat messages
     */
    List<ChatMessageEntity> findByCompanyIdAndCourseIdOrderByTimestampAsc(Long companyId, Long courseId);

    /**
     * Deletes all chat messages older than the specified timestamp.
     *
     * @param timestamp the cutoff timestamp
     */
    void deleteByTimestampBefore(java.time.LocalDateTime timestamp);
}
