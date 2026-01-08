package com.fraud.engine.server.fraudEngineService.jpa.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "frauds_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDecision {
    @javax.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String clientId;
    private int riskScore;

    /**
     * List of fraud reasons. Stored in a separate table 'payment_decision_reasons'
     * linked by 'payment_decision_id'.
     */
    @ElementCollection
    @CollectionTable(
            name = "payment_decision_reasons",           // child table name
            joinColumns = @JoinColumn(name = "payment_decision_id") // FK column
    )
    @Column(name = "reasons")
    private List<String> reasons;

    private Instant createdAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
