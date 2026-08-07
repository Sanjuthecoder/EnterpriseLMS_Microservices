package com.edtech.lms.identity.kafka;

import com.edtech.lms.identity.repositories.CompanyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CompanySubscriptionConsumer — Kafka consumer that upgrades a company's
 * subscription tier to PREMIUM when the payment service confirms a successful payment.
 *
 * Topic: company-premium-upgrade-topic
 * Payload: { "companyId": 123 }
 *
 * Flow:
 * 1. lms-payment-service verifies Razorpay signature → publishes to this topic.
 * 2. This consumer reads the companyId and sets Company.subscriptionTier = "PREMIUM".
 * 3. On next login, AuthenticationService reads the updated tier → embeds in JWT.
 * 4. Gateway forwards X-Subscription-Tier: PREMIUM header.
 * 5. PremiumTierFilter in lms-ai-service allows the request through.
 *
 * Idempotency: Setting PREMIUM on an already-PREMIUM company is a no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySubscriptionConsumer {

    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "company-premium-upgrade-topic",
        groupId = "identity-subscription-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePremiumUpgrade(final String message) {
        log.info("Received premium upgrade event: {}", message);
        try {
            final JsonNode payload = objectMapper.readTree(message);
            final Long companyId = payload.path("companyId").asLong(-1L);

            if (companyId <= 0) {
                log.warn("Invalid companyId in premium upgrade event: {}", message);
                return;
            }

            companyRepository.findById(companyId).ifPresentOrElse(
                company -> {
                    if ("PREMIUM".equals(company.getSubscriptionTier())) {
                        log.info("Company {} is already PREMIUM — skipping.", companyId);
                        return;
                    }
                    company.setSubscriptionTier("PREMIUM");
                    companyRepository.save(company);
                    log.info("Company {} upgraded to PREMIUM successfully.", companyId);
                },
                () -> log.warn("Company {} not found for premium upgrade.", companyId)
            );
        } catch (Exception ex) {
            log.error("Failed to process premium upgrade event: {} — error: {}", message, ex.getMessage());
            // Do NOT rethrow — let Kafka commit the offset to avoid infinite retry loops.
        }
    }
}
