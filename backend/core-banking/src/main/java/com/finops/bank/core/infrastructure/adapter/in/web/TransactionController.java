package com.finops.bank.core.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finops.bank.core.application.port.in.DepositUseCase;
import com.finops.bank.core.application.port.in.DepositUseCase.DepositCommand;
import com.finops.bank.core.application.port.in.WithdrawUseCase;
import com.finops.bank.core.application.port.in.WithdrawUseCase.WithdrawCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;

    public TransactionController(DepositUseCase depositUseCase, WithdrawUseCase withdrawUseCase) {
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
    }

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(@RequestBody @Valid DepositRequest request) {
        DepositCommand command = new DepositCommand(request.accountId(), request.amount());
        depositUseCase.deposit(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@RequestBody @Valid WithdrawRequest request) {
        WithdrawCommand command = new WithdrawCommand(request.accountId(), request.amount());
        withdrawUseCase.withdraw(command);
        return ResponseEntity.ok().build();
    }

    public record DepositRequest(@NotNull UUID accountId, @NotNull @Positive BigDecimal amount) {}
    public record WithdrawRequest(@NotNull UUID accountId, @NotNull @Positive BigDecimal amount) {}
}