package com.finops.bank.risk.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskAssessment {
    private boolean isApproved;
    private String rejectionReason;
    private RiskLevel riskLevel;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}