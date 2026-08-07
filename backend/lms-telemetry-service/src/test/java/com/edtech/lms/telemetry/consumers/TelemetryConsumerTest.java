package com.edtech.lms.telemetry.consumers;

import com.edtech.lms.telemetry.models.XapiStatement;
import com.edtech.lms.telemetry.repositories.XapiStatementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelemetryConsumerTest {

    @Mock
    private XapiStatementRepository xapiStatementRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TelemetryConsumer telemetryConsumer;

    @BeforeEach
    void setUp() {
    }

    @Test
    void consumeXapiStatement_shouldSavePreQuizTelemetry() throws Exception {
        // Arrange
        String message = "{\"employeeId\":2,\"courseId\":1,\"context\":{\"extensions\":{\"quiz_type\":\"PRE_QUIZ\",\"concept\":\"React Basics\"}}}";
        XapiStatement mockStatement = new XapiStatement();
        mockStatement.setEmployeeId(2L);
        mockStatement.setCourseId(1L);

        when(objectMapper.readValue(message, XapiStatement.class)).thenReturn(mockStatement);
        when(xapiStatementRepository.save(any(XapiStatement.class))).thenReturn(mockStatement);

        // Act
        telemetryConsumer.consumeXapiStatement(message);

        // Assert
        ArgumentCaptor<XapiStatement> captor = ArgumentCaptor.forClass(XapiStatement.class);
        verify(xapiStatementRepository, times(1)).save(captor.capture());
        
        XapiStatement saved = captor.getValue();
        assertEquals(2L, saved.getEmployeeId());
        assertEquals(1L, saved.getCourseId());
    }
}
