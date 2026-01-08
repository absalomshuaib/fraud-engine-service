package com.fraud.engine.server.fraudEngineService.entity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentInitiatedRequest {
    private String transactionId;
    private String clientId;
    private BigDecimal amount;
    private String merchantId;
    private String country;
    private Instant timestamp;
    private Double latitude;
    private Double longitude;

    public PaymentInitiatedEvent toEvent() {
        return new PaymentInitiatedEvent(transactionId, clientId, amount, merchantId, country, timestamp,latitude,longitude);
    }
}