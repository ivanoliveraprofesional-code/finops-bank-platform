package com.finops.bank.core.domain.model;

import com.finops.bank.core.domain.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Account {
    private final UUID id;
    private final UUID userId;
    private BigDecimal balance;
    private AccountStatus status;
    private final String currency;
    private LocalDateTime createdAt;

    public Account(UUID userId, String currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.currency = currency;
        this.createdAt = LocalDateTime.now();
    }

    public Account(UUID id, UUID userId, BigDecimal balance, AccountStatus status, String currency, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.status = status;
        this.currency = currency;
        this.createdAt = createdAt;
    }
    
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.balance.compareTo(amount) < 0) throw new InsufficientFundsException("Insufficient funds");
        this.balance = this.balance.subtract(amount);
    }
    
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}	