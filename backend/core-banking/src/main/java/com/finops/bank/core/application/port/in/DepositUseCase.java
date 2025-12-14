package com.finops.bank.core.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepositUseCase {
    void deposit(DepositCommand command);

    record DepositCommand(UUID accountId, BigDecimal amount) {
        public DepositCommand {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) 
                throw new IllegalArgumentException("Deposit amount must be positive");
        }
    }
}