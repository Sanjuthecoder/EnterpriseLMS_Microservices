package com.edtech.lms.payment.services.impl;

import com.edtech.lms.payment.dtos.CreateOrderRequestDto;
import com.edtech.lms.payment.dtos.PaymentVerificationRequestDto;
import com.edtech.lms.payment.entities.PaymentRecord;
import com.edtech.lms.payment.exceptions.PaymentGatewayException;
import com.edtech.lms.payment.exceptions.SignatureMismatchException;
import com.edtech.lms.payment.repositories.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRecordRepository paymentRepository;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "keyId", "rzp_test_TCtsanWkmcb9m2");
        ReflectionTestUtils.setField(paymentService, "keySecret", "lecl8xePgjS7vjeRCe0fR16k");
    }

    @Test
    void testVerifySignature_InvalidSignature() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto("order_1", "pay_1", "invalid_sig", "admin@company.com", "Admin");
        assertThrows(SignatureMismatchException.class, () -> paymentService.verifySignature(request));
        verify(paymentRepository, never()).findByRazorpayOrderId(anyString());
    }

    @Test
    void testCreateOrder_AlreadyPremium() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(1L, 50000, "INR");
        when(paymentRepository.existsByCompanyIdAndStatus(1L, "PAID")).thenReturn(true);
        assertThrows(PaymentGatewayException.class, () -> paymentService.createOrder(request));
    }

    /**
     * Test Corner Case 1: Missing Admin Details.
     * Ensures that if admin email is null, the payment still processes without crashing
     * and without attempting to send a Kafka message.
     */
    @Test
    void testVerifySignature_MissingAdminEmail_DoesNotPublishKafka() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto("order_test", "pay_test", "mock_signature", null, null);
        
        // Mocking Razorpay signature verification logic securely via mocking static Utils
        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), anyString())).thenReturn(true);
            
            PaymentRecord record = PaymentRecord.builder().razorpayOrderId("order_test").status("PENDING").build();
            when(paymentRepository.findByRazorpayOrderId("order_test")).thenReturn(Optional.of(record));

            com.edtech.lms.payment.dtos.PaymentVerificationResponseDto response = paymentService.verifySignature(request);

            org.junit.jupiter.api.Assertions.assertTrue(response.isSuccess());
            verify(paymentRepository, times(1)).save(record);
            // Verify Kafka is NOT called
            verify(kafkaTemplate, never()).send(anyString(), any());
        }
    }

    /**
     * Test Corner Case 2: Duplicate Email Dispatch (Idempotency Risk).
     * Ensures that if the payment is already PAID, we return success early 
     * but DO NOT republish the email notification to Kafka.
     */
    @Test
    void testVerifySignature_AlreadyPaid_Idempotency() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto("order_test", "pay_test", "mock_signature", "admin@company.com", "Admin");
        
        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), anyString())).thenReturn(true);
            
            // Set status to PAID to simulate an already processed order
            PaymentRecord record = PaymentRecord.builder().razorpayOrderId("order_test").status("PAID").build();
            when(paymentRepository.findByRazorpayOrderId("order_test")).thenReturn(Optional.of(record));

            com.edtech.lms.payment.dtos.PaymentVerificationResponseDto response = paymentService.verifySignature(request);

            org.junit.jupiter.api.Assertions.assertTrue(response.isSuccess());
            org.junit.jupiter.api.Assertions.assertEquals("Payment already processed", response.getMessage());
            
            // Verify we do NOT save again and do NOT call Kafka
            verify(paymentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), any());
        }
    }

    /**
     * Test Corner Case 3: Kafka Publishing Failure.
     * Ensures that if Kafka throws an exception, the transaction is gracefully wrapped 
     * in a PaymentGatewayException, triggering a @Transactional rollback.
     */
    @Test
    void testVerifySignature_KafkaFailure_ThrowsExceptionForRollback() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto("order_test", "pay_test", "mock_signature", "admin@company.com", "Admin");
        
        try (org.mockito.MockedStatic<com.razorpay.Utils> mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), anyString())).thenReturn(true);
            
            PaymentRecord record = PaymentRecord.builder().razorpayOrderId("order_test").status("PENDING").build();
            when(paymentRepository.findByRazorpayOrderId("order_test")).thenReturn(Optional.of(record));
            
            // Simulate Kafka failure
            when(kafkaTemplate.send(eq("notification-topic"), any(com.edtech.lms.payment.dtos.NotificationEvent.class)))
                .thenThrow(new RuntimeException("Kafka Broker Down"));

            assertThrows(PaymentGatewayException.class, () -> paymentService.verifySignature(request));
            verify(paymentRepository, times(1)).save(record);
            // Due to @Transactional on the service method, the RuntimeException (wrapped in PaymentGatewayException) 
            // will trigger a rollback in the actual application, keeping data consistent.
        }
    }
}
