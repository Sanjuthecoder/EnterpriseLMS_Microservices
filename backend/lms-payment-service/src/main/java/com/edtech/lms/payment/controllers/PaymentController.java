package com.edtech.lms.payment.controllers;

import com.edtech.lms.payment.dtos.CreateOrderRequestDto;
import com.edtech.lms.payment.dtos.OrderResponseDto;
import com.edtech.lms.payment.dtos.PaymentVerificationRequestDto;
import com.edtech.lms.payment.dtos.PaymentVerificationResponseDto;
import com.edtech.lms.payment.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller handling payment gateway operations.
 * Exposes endpoints for creating orders, verifying payments,
 * and checking subscription status for organizations.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody CreateOrderRequestDto request) {
        OrderResponseDto response = paymentService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verifies the payment signature returned by the client after transaction.
     * 
     * @param request The verification details including the signature.
     * @return 200 OK if the signature is valid.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentVerificationResponseDto> verifyPayment(@Valid @RequestBody PaymentVerificationRequestDto request) {
        PaymentVerificationResponseDto response = paymentService.verifySignature(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the premium subscription status for an organization.
     * 
     * @param companyId The ID of the organization to check.
     * @return A map containing the boolean isPremium flag.
     */
    @GetMapping("/{companyId}/status")
    public ResponseEntity<java.util.Map<String, Boolean>> getPremiumStatus(@PathVariable Long companyId) {
        boolean isPremium = paymentService.isCompanyPremium(companyId);
        return ResponseEntity.ok(java.util.Map.of("isPremium", isPremium));
    }
}
