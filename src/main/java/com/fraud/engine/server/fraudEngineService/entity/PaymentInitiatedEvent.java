package com.fraud.engine.server.fraudEngineService.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiatedEvent {
    private String transactionId;
    private String clientId;
    private BigDecimal amount;
    private String merchantId;
    private String deviceId;
    private String country;
    private Instant timestamp;
    private Double latitude;
    private Double longitude;

    public PaymentInitiatedEvent(String transactionId, String clientId, BigDecimal amount, String merchantId, String country, Instant timestamp,Double latitude, Double longitude) {
        this.transactionId = transactionId;
        this.clientId = clientId;
        this.amount = amount;
        this.merchantId = merchantId;
        this.country = country;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

