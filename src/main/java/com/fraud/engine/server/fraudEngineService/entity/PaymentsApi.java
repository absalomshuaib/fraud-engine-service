package com.fraud.engine.server.fraudEngineService.entity;

public interface PaymentsApi {
    void process(Payment payment);
    void reject(Payment payment);
}

