package com.fraud.engine.server.fraudEngineService.service;

import com.fraud.engine.server.fraudEngineService.entity.FraudResult;
import com.fraud.engine.server.fraudEngineService.entity.Location;
import com.fraud.engine.server.fraudEngineService.entity.Payment;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRulesService {

    private static final int RISK_THRESHOLD = 70;

    private final PaymentRepository paymentRepository;

    public FraudResult evaluate(Payment payment) {

        log.info("Running payment Fraud Rules for paymentID {}", payment.getTransactionId());

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();
        String clientId = payment.getClientId();

        // HIGH AMOUNT ANOMALY
        BigDecimal avgAmount = paymentRepository.getAverageLastMonth(clientId);
        if (avgAmount != null &&
                payment.getAmount().compareTo(avgAmount.multiply(BigDecimal.valueOf(3))) > 0) {

            riskScore += 40;
            reasons.add("HIGH_AMOUNT_ANOMALY");
        }

        // VELOCITY
        int recentCount = paymentRepository.getCountLast3Minutes(clientId);
        if (recentCount > 5) {
            riskScore += 30;
            reasons.add("VELOCITY_FRAUD");
        }

        // DUPLICATE PAYMENT
        Integer duplicateCount = paymentRepository.existsDuplicate(
                clientId,
                payment.getMerchantId(),
                payment.getAmount()
        );

        if (duplicateCount != null && duplicateCount > 0) {
            riskScore += 50;
            reasons.add("DUPLICATE_PAYMENT");
        }

        // GEO-IMPOSSIBLE TRAVEL
        List<Location> locations = paymentRepository.getLocations(clientId);
        if (locations.size() > 1) {
            Location lastLocation = locations.get(1);

            long minutesDiff = Duration
                    .between(lastLocation.getTimestamp(), payment.getTimestamp())
                    .toMinutes();

            if (!isTravelPossible(
                    lastLocation.getLatitude(),
                    lastLocation.getLongitude(),
                    payment.getLatitude(),
                    payment.getLongitude(),
                    minutesDiff)) {

                riskScore += 60;
                reasons.add("GEO_IMPOSSIBLE_TRAVEL");
            }
        }

        // HIGH RISK MERCHANT
        if (merchantRiskScore(payment.getMerchantId()) > 70) {
            riskScore += 30;
            reasons.add("HIGH_RISK_MERCHANT");
        }

        boolean approved = riskScore < RISK_THRESHOLD;

        log.info("Fraud decision={} riskScore={} reasons={}",
                approved ? "APPROVED" : "REJECTED",
                riskScore,
                reasons
        );

        return new FraudResult(approved, riskScore, reasons);
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
        double maxSpeedKmh = 500;
        double maxDistancePossible = maxSpeedKmh * (timeDiffMinutes / 60.0);
        return distanceKm <= maxDistancePossible;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) *
                        Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

