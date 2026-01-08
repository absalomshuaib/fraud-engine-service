package com.fraud.engine.server.fraudEngineService.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentInitiatedResponse {

    private String transactionId;
    private String status;
    private String message;
    private Instant timestamp;
}
