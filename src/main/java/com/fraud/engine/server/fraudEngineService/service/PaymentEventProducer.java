package com.fraud.engine.server.fraudEngineService.service;

import com.fraud.engine.server.fraudEngineService.entity.PaymentDecisionEvent;
import com.fraud.engine.server.fraudEngineService.entity.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
Publishes a “Payment Initiated” event to Kafka
Using the Key event.getTransactionId() and the payload event
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String PAYMENT_DECISION_TOPIC = "payment-decision-topic";

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Publishing Payment to kafka with topic name 'payments-initiated-topic'");
        kafkaTemplate.send("payments-initiated-topic", event.getTransactionId(), event);
    }

    public void publishDecision(PaymentDecisionEvent event) {
        log.info("Publishing PaymentDecisionEvent for transactionId={}",
                event.getTransactionId());

        kafkaTemplate.send(
                PAYMENT_DECISION_TOPIC,
                event.getClientId(),
                event
        );
    }
}
