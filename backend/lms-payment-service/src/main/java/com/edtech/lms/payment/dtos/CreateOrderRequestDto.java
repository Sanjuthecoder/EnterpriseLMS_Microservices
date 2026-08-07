package com.edtech.lms.payment.dtos;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class CreateOrderRequestDto {
    @NotNull(message = "Company ID is required")
    private final Long companyId;
    @NotNull(message = "Amount is required")
    private final Integer amount;
    @NotNull(message = "Currency is required")
    private final String currency;
}
