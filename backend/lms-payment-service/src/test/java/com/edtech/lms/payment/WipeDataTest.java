package com.edtech.lms.payment;

import com.edtech.lms.payment.repositories.PaymentRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WipeDataTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate kafkaTemplate;

    @Autowired
    private PaymentRecordRepository repository;

    @Test
    public void wipeData() {
        System.out.println("Starting data wipe...");
        repository.deleteAll();
        System.out.println("Data wiped.");
    }
}
