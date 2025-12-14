package com.finops.bank.core.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountTransactionEvent(
    UUID eventId,
    UUID accountId,
    String type,
    BigDecimal amount,
    LocalDateTime timestamp
) {}