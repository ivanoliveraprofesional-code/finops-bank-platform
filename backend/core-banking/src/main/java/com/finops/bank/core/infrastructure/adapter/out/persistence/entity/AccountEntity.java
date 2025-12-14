package com.finops.bank.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    @Id
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private String status;
    private String currency;
    private LocalDateTime createdAt;
}