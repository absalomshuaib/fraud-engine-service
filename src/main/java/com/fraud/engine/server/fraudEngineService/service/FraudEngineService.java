package com.fraud.engine.server.fraudEngineService.service;

import com.fraud.engine.server.fraudEngineService.entity.Decision;
import com.fraud.engine.server.fraudEngineService.entity.Location;
import com.fraud.engine.server.fraudEngineService.entity.PaymentDecisionEvent;
import com.fraud.engine.server.fraudEngineService.entity.PaymentInitiatedEvent;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Listens for when a event is recieved Receives “payments-initiated-topic"
Using the Key event.getTransactionId() and the payload event
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEngineService {

    private static final int RISK_THRESHOLD = 70;
    // JPA repo for payments table
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentDecisionEvent> kafkaTemplate;

    @KafkaListener(topics = "payments-initiated-topic", groupId = "fraud-engine")
    public void handlePayment(PaymentInitiatedEvent payment) {
        log.info("Running payment Fraud Rules for paymentID " + payment.getTransactionId());
        int riskScore = 0;
        List<String> reasons = new ArrayList<>();
        String clientId = payment.getClientId();

        //High Amount Anomaly: (compare to last month average)
        BigDecimal avgAmount = paymentRepository.getAverageLastMonth(clientId);
        if (avgAmount != null && payment.getAmount().compareTo(avgAmount.multiply(BigDecimal.valueOf(3))) > 0) {
            riskScore += 40;
            reasons.add("HIGH_AMOUNT_ANOMALY");
            log.info("High Amount Anomaly detected for " + payment.getTransactionId());

        }

        // High VELOCITY Fraud (transactions in last 3 minutes)
        int recentCount = paymentRepository.getCountLast3Minutes(clientId);
        if (recentCount > 5) {
            log.info("High VELOCITY transactions detected for " + payment.getTransactionId());
            riskScore += 30;
            reasons.add("VELOCITY_FRAUD");
        }

        // Duplicate payments (transactions in last 3 minutes )
        // Check if a duplicate payment exists (same client, merchant, amount in last 3 min)
        Integer duplicateCount = paymentRepository.existsDuplicate(
                payment.getClientId(),
                payment.getMerchantId(),
                payment.getAmount()
        );

        boolean duplicate = duplicateCount != null && duplicateCount > 0;

        if (duplicate) {
            log.info("Duplicate transactions detected for " + payment.getTransactionId());
            riskScore += 50;
            reasons.add("DUPLICATE_PAYMENT");
        }

        //Geo-Impossible Travel
        //Check if payment is within 24 hour since previous payment to be realistic
        //Check if location is within car or plane range as per KM
        //then adjust the max speed per car or plane to determine best method
        log.info("Fraud Rule LastLocation travel: " + payment.getTransactionId());
        List<Location> locations = paymentRepository.getLocations(clientId);
        //Get all locations for this client ordered by timestamp DESC
        if (!locations.isEmpty()) {
            log.info("locations: " + locations);
            // Pick the most recent location would be 1 here as 0 would be the current initiated maybe also check status completed ?
            Location lastLocation = locations.get(1);

            Double lastLat = lastLocation.getLatitude();
            Double lastLon = lastLocation.getLongitude();

            //Calculate minutes since last payment
            long minutesSinceLastPayment = java.time.Duration
                    .between(lastLocation.getTimestamp(), payment.getTimestamp())
                    .toMinutes();
            log.info("minutesSinceLastPayment: " + minutesSinceLastPayment);

            //Check if travel is possible
            if (!isTravelPossible(lastLat, lastLon, payment.getLatitude(), payment.getLongitude(), minutesSinceLastPayment)) {
                log.info("Payment from previous location might be impossible → marking as high risk");
                riskScore += 60;
                reasons.add("GEO_IMPOSSIBLE_TRAVEL");
            } else {
                log.info("Payment from previous location is possible");
            }
        } else {
            log.info("No previous payment location found for client: " + clientId);
        }

        // Merchant High Risk (hard coded blacklist of merchants)
        int merchantRisk = merchantRiskScore(payment.getMerchantId());
        if (merchantRisk > 70) {
            log.info("High risk merchant detected for " + payment.getTransactionId());
            riskScore += 30;
            reasons.add("HIGH_RISK_MERCHANT");
        }

        // Make decision based of the risk score associated
        Decision decision = (riskScore >= RISK_THRESHOLD) ? Decision.REJECTED : Decision.APPROVED;
        log.info("Fraud checks completed: Decision =  " + decision);
        log.info("Fraud checks completed: RiskScore =  " + riskScore);
        log.info("Fraud checks completed: Reasons =  " + reasons);

        // build the PaymentDecisionEvent object
        PaymentDecisionEvent decisionEvent = PaymentDecisionEvent.builder()
                .transactionId(payment.getTransactionId())
                .clientId(clientId)
                .decision(decision)
                .riskScore(riskScore)
                .reasons(reasons)
                .decisionTime(Instant.now())
                .build();

        log.info("Publishing decision to topic event payment-decision-topic");

        kafkaTemplate.send("payment-decision-topic", clientId, decisionEvent);
    }

    private int merchantRiskScore(String merchantId) {
        log.info("Merchant-ID: " + merchantId);
        Map<String, Integer> riskMap = new HashMap<>();
        riskMap.put("M123", 80);
        riskMap.put("M999", 90);
        if (merchantId == null) return 10;
        return riskMap.getOrDefault(merchantId.toUpperCase(), 10);
    }

    private boolean isTravelPossible(Double lastLat, Double lastLon,
                                     Double currentLat, Double currentLon,
                                     long timeDiffMinutes) {
        double distanceKm = haversine(lastLat, lastLon, currentLat, currentLon);
        double maxSpeedKmh = 500; // e.g., max realistic speed by plane e.g can also check for vehicle or walking distance ect
        double maxDistancePossible = maxSpeedKmh * (timeDiffMinutes / 60.0);
        log.info("distanceKm: " + distanceKm);
        log.info("maxDistancePossible: " + maxDistancePossible);

        return distanceKm <= maxDistancePossible;
    }

    // Haversine formula: This is a Haversine distance calculation —
    // A standard mathematical formula used to calculate the real distance between two locations on Earth using latitude and longitude
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}