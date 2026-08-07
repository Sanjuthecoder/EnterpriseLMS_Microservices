package com.edtech.lms.course.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Long lessonId;
    private String title;
    private String moduleTitle;
    private String description;
    private String lessonType;
    private Integer seqOrder;
}
