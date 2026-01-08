package com.fraud.engine.server.fraudEngineService.entity;


import lombok.*;
import org.springframework.data.annotation.Id;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @javax.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String transactionId;

    @Column(nullable=false)
    private String clientId;

    @Column(nullable=false)
    private BigDecimal amount;

    private String merchantId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    private Instant createdAt;
    private Instant updatedAt;
    @Column
    private Double latitude;

    @Column
    private Double longitude;
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

