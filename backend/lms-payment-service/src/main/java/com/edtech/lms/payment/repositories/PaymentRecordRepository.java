package com.edtech.lms.payment.repositories;

import com.edtech.lms.payment.entities.PaymentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PaymentRecordRepository extends MongoRepository<PaymentRecord, String> {
    Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId);
    boolean existsByCompanyIdAndStatus(Long companyId, String status);
}
