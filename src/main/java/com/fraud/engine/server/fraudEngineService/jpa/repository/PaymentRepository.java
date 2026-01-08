package com.fraud.engine.server.fraudEngineService.jpa.repository;

import com.fraud.engine.server.fraudEngineService.entity.Location;
import com.fraud.engine.server.fraudEngineService.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// table payments using JPQL(Java Persistence Query Language) — a query language that uses entity names and fields, not table/column names.
//Hence the JPQL will be converted into SQL by Hibernate at runtime
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);

    // Average amount for the last 30 days for a client
    @Query(value = "SELECT AVG(p.amount) FROM payment p " +
            "WHERE p.client_id = :clientId AND p.timestamp >= NOW() - INTERVAL 30 DAY",
            nativeQuery = true)
    BigDecimal getAverageLastMonth(String clientId);

    // Count of payments in the last 3 minutes for velocity check
    @Query(value = "SELECT COUNT(*) FROM payment p " +
            "WHERE p.client_id = :clientId AND p.timestamp >= NOW() - INTERVAL 3 MINUTE",
            nativeQuery = true)
    int getCountLast3Minutes(String clientId);

    //Check if a duplicate payment exists (same client, merchant, amount in last 3 min)
    @Query(value = "SELECT CASE WHEN COUNT(*) > 2 THEN TRUE ELSE FALSE END FROM payment p " +
            "WHERE p.client_id = :clientId " +
            "AND p.merchant_id = :merchantId " +
            "AND p.amount = :amount " +
            "AND p.timestamp >= NOW() - INTERVAL 3 MINUTE",
            nativeQuery = true)
    Integer existsDuplicate(String clientId, String merchantId, BigDecimal amount);

    //Get last known country for Geo-Impossible Travel
    @Query("SELECT new com.fraud.engine.server.fraudEngineService.entity.Location(p.latitude, p.longitude, p.timestamp) " +
            "FROM Payment p WHERE p.clientId = :clientId ORDER BY p.timestamp DESC")
    List<Location> getLocations(String clientId);
}

