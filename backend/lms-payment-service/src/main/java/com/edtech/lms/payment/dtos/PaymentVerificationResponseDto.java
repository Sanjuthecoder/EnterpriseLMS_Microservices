package com.edtech.lms.payment.dtos;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class PaymentVerificationResponseDto {
    private final boolean success;
    private final String message;
}
