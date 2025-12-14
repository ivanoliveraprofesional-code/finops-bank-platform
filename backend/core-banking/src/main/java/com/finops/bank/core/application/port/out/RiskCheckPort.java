package com.finops.bank.core.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RiskCheckPort {
    RiskCheckResult checkRisk(UUID userId, BigDecimal amount);

    record RiskCheckResult(boolean isApproved, String rejectionReason) {}
}