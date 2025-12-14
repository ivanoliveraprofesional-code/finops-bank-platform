package com.finops.bank.audit.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionEventDto(
    UUID eventId,
    UUID accountId,
    String type,
    BigDecimal amount,
    LocalDateTime timestamp
) {}