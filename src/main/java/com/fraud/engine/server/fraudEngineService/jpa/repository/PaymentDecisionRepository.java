package com.fraud.engine.server.fraudEngineService.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentDecisionRepository extends JpaRepository<PaymentDecision, Long> {
    Optional<PaymentDecision> findTopByTransactionIdOrderByCreatedAtDesc(String transactionId);

}
