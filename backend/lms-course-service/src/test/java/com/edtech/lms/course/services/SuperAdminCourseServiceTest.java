package com.edtech.lms.course.services;

import com.edtech.lms.course.models.entities.CourseStructure;
import com.edtech.lms.course.models.enums.ContentType;
import com.edtech.lms.course.repositories.CourseStructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminCourseServiceTest {

    @Mock
    private CourseStructureRepository courseStructureRepository;

    @InjectMocks
    private SuperAdminCourseService superAdminCourseService;

    @Test
    void getFirstVideoLessonId_ShouldReturnFirstVideoLessonId() {
        Long courseId = 1L;
        CourseStructure pdfLesson = new CourseStructure();
        pdfLesson.setLessonId(10L);
        pdfLesson.setLessonType(ContentType.PDF);

        CourseStructure videoLesson1 = new CourseStructure();
        videoLesson1.setLessonId(20L);
        videoLesson1.setLessonType(ContentType.VIDEO);

        CourseStructure videoLesson2 = new CourseStructure();
        videoLesson2.setLessonId(30L);
        videoLesson2.setLessonType(ContentType.VIDEO);

        when(courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId))
                .thenReturn(List.of(pdfLesson, videoLesson1, videoLesson2));

        Long result = superAdminCourseService.getFirstVideoLessonId(courseId);

        assertNotNull(result);
        assertEquals(20L, result);
    }

    @Test
    void getFirstVideoLessonId_ShouldReturnNullWhenNoVideoLesson() {
        Long courseId = 2L;
        CourseStructure pdfLesson = new CourseStructure();
        pdfLesson.setLessonId(10L);
        pdfLesson.setLessonType(ContentType.PDF);

        when(courseStructureRepository.findByCourseIdOrderBySeqOrder(courseId))
                .thenReturn(List.of(pdfLesson));

        Long result = superAdminCourseService.getFirstVideoLessonId(courseId);

        assertNull(result);
    }
}
