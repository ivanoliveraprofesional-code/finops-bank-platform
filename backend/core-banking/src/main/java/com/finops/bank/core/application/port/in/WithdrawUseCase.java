package com.finops.bank.core.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawUseCase {
    void withdraw(WithdrawCommand command);

    record WithdrawCommand(UUID accountId, BigDecimal amount) {
        public WithdrawCommand {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) 
                throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
    }
}