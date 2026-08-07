package com.edtech.lms.payment.dtos;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class PaymentVerificationRequestDto {
    @NotBlank(message = "Order ID is required")
    private final String razorpayOrderId;
    @NotBlank(message = "Payment ID is required")
    private final String razorpayPaymentId;
    @NotBlank(message = "Signature is required")
    private final String razorpaySignature;
    private final String adminEmail;
    private final String adminName;
}
