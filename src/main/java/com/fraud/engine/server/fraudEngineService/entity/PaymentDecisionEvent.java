package com.fraud.engine.server.fraudEngineService.entity;

import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDecisionEvent {
    private String transactionId;
    private String clientId;
    private Decision decision;
    private int riskScore;
    private List<String> reasons;
    private Instant decisionTime;

    public static PaymentDecisionEvent from(
            Payment payment,
            PaymentDecision decisionEntity,
            FraudResult fraudResult) {

        return PaymentDecisionEvent.builder()
                .transactionId(payment.getTransactionId())
                .clientId(payment.getClientId())
                .decision(fraudResult.isApproved() ? Decision.APPROVED : Decision.REJECTED)
                .riskScore(fraudResult.getRiskScore())
                .reasons(decisionEntity.getReasons())
                .decisionTime(Instant.now())
                .build();
    }
}

