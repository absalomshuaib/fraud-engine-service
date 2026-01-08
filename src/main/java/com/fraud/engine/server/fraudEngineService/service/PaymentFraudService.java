package com.fraud.engine.server.fraudEngineService.service;

import com.fraud.engine.server.fraudEngineService.entity.*;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecision;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecisionRepository;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFraudService {

    private final PaymentRepository paymentRepository;
    private final PaymentDecisionRepository decisionRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final FraudRulesService fraudRulesEngine;

    public PaymentDecisionResult evaluate(PaymentInitiatedRequest request) {

        Payment payment = Payment.builder()
                .transactionId(request.getTransactionId())
                .clientId(request.getClientId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .timestamp(request.getTimestamp())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        paymentRepository.save(payment);

        //Run fraud rules synchronously
        FraudResult fraudResult = fraudRulesEngine.evaluate(payment);

        PaymentDecision decision = PaymentDecision.builder()
                .transactionId(payment.getTransactionId())
                .clientId(payment.getClientId())
                .riskScore(fraudResult.getRiskScore())
                .reasons(fraudResult.getReasons())
                .createdAt(Instant.now())
                .build();

        decisionRepository.saveAndFlush(decision);

        payment.setStatus(
                fraudResult.isApproved()
                        ? PaymentStatus.APPROVED
                        : PaymentStatus.REJECTED
        );

        paymentRepository.save(payment);

        //Publish async event
        paymentEventProducer.publishDecision(
                PaymentDecisionEvent.from(payment, decision, fraudResult)
        );
        // return the response
        return new PaymentDecisionResult(payment, decision, fraudResult);
    }
}
