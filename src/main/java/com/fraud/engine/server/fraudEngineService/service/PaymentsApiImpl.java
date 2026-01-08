package com.fraud.engine.server.fraudEngineService.service;

import com.fraud.engine.server.fraudEngineService.entity.Payment;
import com.fraud.engine.server.fraudEngineService.entity.PaymentStatus;
import com.fraud.engine.server.fraudEngineService.entity.PaymentsApi;
import com.fraud.engine.server.fraudEngineService.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentsApiImpl implements PaymentsApi {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    //ensures your database operations only run if nothign fails, which is critical for financial data, if something fails it wont save anything to do and keep it in an inconsistent state
    public void process(Payment payment) {
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            log.error("Payment Financial {} failed since payment is not in INITIATED state but also hasnt been processed yet for status " + payment.getStatus(), payment.getTransactionId());
            return;
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        try {
            log.error("Performing Payment Financial", payment.getTransactionId());
            PaymentsFinancialApi(payment);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setUpdatedAt(Instant.now());
            paymentRepository.save(payment);
            log.info("Payment Financial {} COMPLETED", payment.getTransactionId());
        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.REJECTED);
            payment.setUpdatedAt(Instant.now());
            paymentRepository.save(payment);
            log.error("Payment Financial {} failed", payment.getTransactionId());
        }
    }

    @Override
    @Transactional
    public void reject(Payment payment) {
        if (payment.getStatus() != PaymentStatus.INITIATED) return;
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Payment Financial {} REJECTED", payment.getTransactionId());
    }

    private void PaymentsFinancialApi(Payment payment) throws InterruptedException {
        //This will be the actual financial api called to perform the payment
        Thread.sleep(500);
    }
}
