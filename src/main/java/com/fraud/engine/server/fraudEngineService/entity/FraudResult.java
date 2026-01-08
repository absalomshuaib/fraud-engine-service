package com.fraud.engine.server.fraudEngineService.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FraudResult {

    private final boolean approved;
    private final int riskScore;
    private final List<String> reasons;

}