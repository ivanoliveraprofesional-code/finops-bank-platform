package com.finops.bank.core.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetAccountUseCase {
    AccountResponse getAccount(UUID id);

    record AccountResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        String status
    ) {}
}