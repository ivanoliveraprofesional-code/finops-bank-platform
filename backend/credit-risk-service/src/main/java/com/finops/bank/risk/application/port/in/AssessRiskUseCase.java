package com.finops.bank.risk.application.port.in;

import com.finops.bank.risk.domain.model.RiskAssessment;
import java.math.BigDecimal;
import java.util.UUID;

public interface AssessRiskUseCase {
    RiskAssessment assess(AssessCommand command);

    record AssessCommand(UUID userId, BigDecimal amount) {
        public AssessCommand {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) 
                throw new IllegalArgumentException("Amount must be positive");
        }
    }
}