package com.edtech.lms.payment.dtos;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class OrderResponseDto {
    private final String razorpayOrderId;
    private final Integer amount;
    private final String currency;
}
