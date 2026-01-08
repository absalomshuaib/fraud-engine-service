package com.fraud.engine.server.fraudEngineService.rest;

import com.fraud.engine.server.fraudEngineService.entity.*;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecision;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentDecisionRepository;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import com.fraud.engine.server.fraudEngineService.service.PaymentEventProducer;
import com.fraud.engine.server.fraudEngineService.service.PaymentFraudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/*
docker-compose -f kafka-compose.yml up -d

(This is Johannesburg)
curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePayment \
-H "Content-Type: application/json" \
-d '{
  "transactionId": "TXN10001",
  "clientId": "C123",
  "amount": 5000,
  "merchantId": "M123",
  "country": "ZA",
  "timestamp": "2025-12-14T08:00:00Z",
  "latitude": -26.2041,
  "longitude": 28.0473
}'

(This is Cape Town)
curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePayment \
-H "Content-Type: application/json" \
-d '{
  "transactionId": "TXN10002",
  "clientId": "C123",
  "amount": 2000,
  "merchantId": "M456",
  "country": "ZA",
  "timestamp": "2025-12-14T08:30:00Z",
  "latitude": -33.9249,
  "longitude": 18.4241
}'
*/

/*
Rest Class: Api to initiate the payment Fraud rules API
*/
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payments")
public class PaymentRestController {

    //service class to send the topic event
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentRepository paymentRepository;
    private final PaymentDecisionRepository decisionRepository;

    private final PaymentFraudService fraudService;


    //asych api approuch to make a payment/fraud checks/kafka
    @PostMapping("/initiatePayment")
    public ResponseEntity<PaymentInitiatedResponse> initiatePayment(
            @RequestBody PaymentInitiatedRequest request) {

        log.info("Initiating payment Fraud process");
        //Saves the payment data into the payment table as INITIATED

        // Check if the transaction already exists
        boolean exists = paymentRepository.findByTransactionId(request.getTransactionId()).isPresent();
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(PaymentInitiatedResponse.builder()
                            .status("ERROR")
                            .message("TransactionId already found: " + request.getTransactionId())
                            .transactionId(request.getTransactionId())
                            .timestamp(request.getTimestamp())
                            .build());
        }
        Payment payment = new Payment();
        payment.setTransactionId(request.getTransactionId());
        payment.setClientId(request.getClientId());
        payment.setMerchantId(request.getMerchantId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.valueOf("INITIATED"));
        payment.setTimestamp(request.getTimestamp());
        payment.setLatitude(request.getLatitude());
        payment.setLongitude(request.getLongitude());

        paymentRepository.save(payment);
        log.info("Payment stored as as INITIATED for TransID " + request.getTransactionId());

        //Convert request to an event to be published to kafka
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                request.getTransactionId(),
                request.getClientId(),
                request.getAmount(),
                request.getMerchantId(),
                request.getCountry(),
                request.getTimestamp(),
                request.getLatitude(),
                request.getLongitude()
        );
        log.info("Publishing Payment to service/kafka");

        //Publish event
        paymentEventProducer.publishPaymentInitiated(event);

        // Build response
        PaymentInitiatedResponse response = PaymentInitiatedResponse.builder()
                .transactionId(request.getTransactionId())
                .status("SUCCESS")
                .message("Payment initiated completed")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    // Returns payment fraud status detailed response
    @GetMapping("/getPaymentDecision/{transactionId}")
    public ResponseEntity<PaymentDetailsResponse> getPaymentDetails(
            @PathVariable String transactionId) {

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found for transactionId: " + transactionId
                ));

        // Fetch latest decision
        PaymentDecision decision = decisionRepository
                .findTopByTransactionIdOrderByCreatedAtDesc(transactionId)
                .orElse(null);

        // return transaction from payments table
        PaymentDetailsResponse response = PaymentDetailsResponse.builder()
                .transactionId(payment.getTransactionId())
                .clientId(payment.getClientId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .latitude(payment.getLatitude())
                .longitude(payment.getLongitude())
                .initiatedAt(payment.getTimestamp())
                .lastUpdatedAt(payment.getUpdatedAt())
                .riskScore(decision != null ? decision.getRiskScore() : null)
                .reasons(decision != null ? decision.getReasons() : null)
                .build();

        return ResponseEntity.ok(response);
    }
/*
    curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePaymentDecision \
            -H "Content-Type: application/json" \
            -d '{
            "transactionId": "TXN10001111111",
            "clientId": "C123",
            "amount": 5000,
            "merchantId": "M123",
            "country": "ZA",
            "timestamp": "2025-12-14T08:00:00Z",
            "latitude": -26.2041,
            "longitude": 28.0473
}'
/*
 */
    //asych api approach to return the decision result from the fraud checks and make the payment on the payment service application
    @PostMapping("/initiatePaymentDecision")
    public ResponseEntity<PaymentDetailsResponse> initiatePaymentDecision(
            @RequestBody PaymentInitiatedRequest request) {

        // Check if the transaction already exists
        boolean exists = paymentRepository.findByTransactionId(request.getTransactionId()).isPresent();
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(PaymentDetailsResponse.builder()
                            .statusMessage("ERROR")
                            .message("TransactionId already found: " + request.getTransactionId())
                            .transactionId(request.getTransactionId())
                            .timestamp(request.getTimestamp())
                            .build());
        }

        PaymentDecisionResult result = fraudService.evaluate(request);

        PaymentDetailsResponse response = PaymentDetailsResponse.builder()
                .transactionId(result.getPayment().getTransactionId())
                .clientId(result.getPayment().getClientId())
                .amount(result.getPayment().getAmount())
                .status(PaymentStatus.valueOf(result.getPayment().getStatus().name()))
                .riskScore(result.getDecision().getRiskScore())
                .reasons(result.getDecision().getReasons())
                .lastUpdatedAt(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }
}
