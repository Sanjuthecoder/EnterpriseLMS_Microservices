package com.edtech.lms.telemetry.controllers;

import com.edtech.lms.telemetry.models.VideoTelemetry;
import com.edtech.lms.telemetry.models.dtos.responses.VideoTelemetryResponse;
import com.edtech.lms.telemetry.repositories.VideoTelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TelemetryControllerTest {

    @Mock
    private VideoTelemetryRepository videoTelemetryRepository;

    @InjectMocks
    private TelemetryController telemetryController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetVideoSessions() {
        VideoTelemetry tel = new VideoTelemetry();
        tel.setEmployeeId(1L);
        tel.setCompletionPercentage(50);

        when(videoTelemetryRepository.findByEmployeeIdAndCourseId(1L, 2L))
            .thenReturn(java.util.Collections.singletonList(tel));

        ResponseEntity<java.util.List<VideoTelemetryResponse>> res = telemetryController.getVideoSessions(1L, 2L);
        assertNotNull(res);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(50, res.getBody().get(0).getCompletionPercentage());
    }
}
