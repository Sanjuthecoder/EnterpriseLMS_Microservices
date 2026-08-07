package com.edtech.lms.payment.services.impl;

import com.edtech.lms.payment.dtos.CreateOrderRequestDto;
import com.edtech.lms.payment.dtos.OrderResponseDto;
import com.edtech.lms.payment.dtos.PaymentVerificationRequestDto;
import com.edtech.lms.payment.dtos.PaymentVerificationResponseDto;
import com.edtech.lms.payment.entities.PaymentRecord;
import com.edtech.lms.payment.exceptions.PaymentGatewayException;
import com.edtech.lms.payment.exceptions.SignatureMismatchException;
import com.edtech.lms.payment.repositories.PaymentRecordRepository;
import com.edtech.lms.payment.services.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of PaymentService managing Razorpay integration.
 * Handles the creation of orders and validation of signatures,
 * acting as the primary business logic layer for premium subscriptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordRepository paymentRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Override
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto request) {
        if (isCompanyPremium(request.getCompanyId())) {
            throw new PaymentGatewayException("Company is already a premium member.");
        }
        
        try {
            String orderId = executeRazorpayOrder(request);
            savePendingPaymentRecord(request, orderId);

            log.info("Order created successfully: {}", orderId);
            return OrderResponseDto.builder()
                    .razorpayOrderId(orderId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to create order with Razorpay");
        }
    }

    private String executeRazorpayOrder(CreateOrderRequestDto request) throws com.razorpay.RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount() * 100); // Amount in paise
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = razorpay.orders.create(orderRequest);
        return order.get("id");
    }

    private void savePendingPaymentRecord(CreateOrderRequestDto request, String orderId) {
        PaymentRecord record = PaymentRecord.builder()
                .companyId(request.getCompanyId())
                .razorpayOrderId(orderId)
                .status("PENDING")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(record);
    }

    @Override
    @Transactional
    public PaymentVerificationResponseDto verifySignature(PaymentVerificationRequestDto request) {
        try {
            validateRazorpaySignature(request);

            PaymentRecord record = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new PaymentGatewayException("Order not found"));

            if ("PAID".equals(record.getStatus())) {
                return PaymentVerificationResponseDto.builder().success(true).message("Payment already processed").build();
            }

            updatePaymentRecordToPaid(record, request.getRazorpayPaymentId());
            publishPostPaymentEvents(request, record.getCompanyId());

            log.info("Payment verified and recorded for order: {}", request.getRazorpayOrderId());
            return PaymentVerificationResponseDto.builder().success(true).message("Payment verified successfully").build();
        } catch (SignatureMismatchException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Payment verification failed");
        }
    }

    private void validateRazorpaySignature(PaymentVerificationRequestDto request) {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        try {
            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            if (!isValid) {
                throw new SignatureMismatchException("Invalid signature detected");
            }
        } catch (Exception e) {
            if (e instanceof SignatureMismatchException) throw (SignatureMismatchException)e;
            throw new PaymentGatewayException("Failed to verify signature", e);
        }
    }

    private void updatePaymentRecordToPaid(PaymentRecord record, String razorpayPaymentId) {
        record.setRazorpayPaymentId(razorpayPaymentId);
        record.setStatus("PAID");
        record.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(record);
    }

    private void publishPostPaymentEvents(PaymentVerificationRequestDto request, Long companyId) {
        if (request.getAdminEmail() != null && !request.getAdminEmail().isEmpty()) {
            com.edtech.lms.payment.dtos.NotificationEvent event = new com.edtech.lms.payment.dtos.NotificationEvent(
                    "PREMIUM_UPGRADE",
                    request.getAdminEmail(),
                    java.util.Map.of("username", request.getAdminName() != null ? request.getAdminName() : "Admin")
            );
            kafkaTemplate.send("notification-topic", event);
            log.info("Published PREMIUM_UPGRADE event to notification-topic for {}", request.getAdminEmail());
        }

        try {
            String upgradePayload = String.format("{\"companyId\":%d}", companyId);
            kafkaTemplate.send("company-premium-upgrade-topic", upgradePayload);
            log.info("Published premium upgrade event to identity service for companyId={}", companyId);
        } catch (Exception kafkaEx) {
            log.error("Failed to publish premium upgrade Kafka event: {}", kafkaEx.getMessage());
        }
    }

    @Override
    public boolean isCompanyPremium(Long companyId) {
        return paymentRepository.existsByCompanyIdAndStatus(companyId, "PAID");
    }
}
