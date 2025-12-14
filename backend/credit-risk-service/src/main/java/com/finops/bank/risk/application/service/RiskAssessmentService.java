package com.finops.bank.risk.application.service;

import com.finops.bank.risk.application.port.in.AssessRiskUseCase;
import com.finops.bank.risk.domain.model.RiskAssessment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class RiskAssessmentService implements AssessRiskUseCase {

    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("5000.00");

    @Override
    public RiskAssessment assess(AssessCommand command) {
        log.info("Assessing risk for User: {} Amount: {}", command.userId(), command.amount());

        if (command.amount().compareTo(HIGH_RISK_THRESHOLD) > 0) {
            log.warn("High risk detected for transaction > 5000");
            return RiskAssessment.builder()
                    .isApproved(false)
                    .riskLevel(RiskAssessment.RiskLevel.HIGH)
                    .rejectionReason("Transaction exceeds automatic approval limit of 5000")
                    .build();
        }

        return RiskAssessment.builder()
                .isApproved(true)
                .riskLevel(RiskAssessment.RiskLevel.LOW)
                .rejectionReason(null)
                .build();
    }
} 