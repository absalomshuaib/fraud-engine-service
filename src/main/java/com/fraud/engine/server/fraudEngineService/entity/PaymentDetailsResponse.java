package com.fraud.engine.server.fraudEngineService.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentDetailsResponse {

    private String transactionId;
    private String clientId;
    private String merchantId;
    private BigDecimal amount;

    private PaymentStatus status;

    private Double latitude;
    private Double longitude;

    private Instant initiatedAt;
    private Instant lastUpdatedAt;
    private Integer riskScore;
    private List<String> reasons;
    private String statusMessage;
    private String message;
    private Instant timestamp;

}
