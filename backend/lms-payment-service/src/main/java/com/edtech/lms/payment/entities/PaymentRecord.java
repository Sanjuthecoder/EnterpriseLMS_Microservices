package com.edtech.lms.payment.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "payment_records")
public class PaymentRecord {
    @Id
    private String id;
    private Long companyId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status;
    private Integer amount;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
