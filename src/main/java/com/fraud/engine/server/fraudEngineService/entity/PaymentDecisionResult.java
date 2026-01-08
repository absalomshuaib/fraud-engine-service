package com.fraud.engine.server.fraudEngineService.entity;

import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecision;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentDecisionResult {

    private final Payment payment;
    private final PaymentDecision decision;
    private final FraudResult fraudResult;

}
