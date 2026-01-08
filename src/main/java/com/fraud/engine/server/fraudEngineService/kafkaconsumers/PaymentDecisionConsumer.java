package com.fraud.engine.server.fraudEngineService.kafkaconsumers;

import com.fraud.engine.server.fraudEngineService.entity.Decision;
import com.fraud.engine.server.fraudEngineService.entity.Payment;
import com.fraud.engine.server.fraudEngineService.entity.PaymentDecisionEvent;
import com.fraud.engine.server.fraudEngineService.entity.PaymentsApi;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecision;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecisionRepository;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/*
Listens for when a event is recieved Receives “payment-decision-topic"
Using the Key event.getTransactionId() and the payload event
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDecisionConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentsApi paymentsApi;
    private final PaymentDecisionRepository decisionRepository;
    @KafkaListener(topics = "payment-decision-topic", groupId = "payments-service")
    public void handleDecision(PaymentDecisionEvent event) {

        // find the transactionID from the event to return the payment object
        Payment payment = paymentRepository
                .findByTransactionId(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for transaction: " + event.getTransactionId()));
        // Save the decision to fraud_decisions DB

        //the builder() is a clean automatic way of creating a Java object instead of calling/instantiating a new object + setters.
        //using Lombok
        PaymentDecision decision = PaymentDecision.builder()
                .transactionId(event.getTransactionId())
                .clientId(payment.getClientId())
                .riskScore(event.getRiskScore())
                .reasons(event.getReasons())
                .createdAt(Instant.now())
                .build();
        log.info("Saving decision to topic event payment-decision-topic");
        decisionRepository.save(decision);

        if (event.getDecision()  == Decision.APPROVED) {
            log.info("Payment process");
            paymentsApi.process(payment);
        } else {
            log.info("Payment reject");
            paymentsApi.reject(payment);
        }
    }
}

