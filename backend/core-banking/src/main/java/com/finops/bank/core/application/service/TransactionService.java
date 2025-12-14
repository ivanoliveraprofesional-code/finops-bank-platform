package com.finops.bank.core.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finops.bank.core.application.port.in.DepositUseCase;
import com.finops.bank.core.application.port.in.WithdrawUseCase;
import com.finops.bank.core.application.port.out.LoadAccountPort;
import com.finops.bank.core.application.port.out.PublishTransactionPort;
import com.finops.bank.core.application.port.out.RiskCheckPort;
import com.finops.bank.core.application.port.out.SaveAccountPort;
import com.finops.bank.core.domain.event.AccountTransactionEvent;
import com.finops.bank.core.domain.model.Account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService implements DepositUseCase, WithdrawUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final PublishTransactionPort publishTransactionPort;
    private final RiskCheckPort riskCheckPort;

    @Override
    public void deposit(DepositCommand command) {
        log.info("Processing Deposit: {} to Account: {}", command.amount(), command.accountId());
        executeTransaction(command.accountId(), account -> account.deposit(command.amount()));
        finalizeTransaction(command.accountId(), "DEPOSIT", command.amount());
    }

    @Override
    public void withdraw(WithdrawCommand command) {
        log.info("Processing Withdrawal: {} from Account: {}", command.amount(), command.accountId());
        
        Account account = getAccountOrThrow(command.accountId());

        var riskResult = riskCheckPort.checkRisk(account.getUserId(), command.amount());

        if (!riskResult.isApproved()) {
            log.warn("Transaction Rejected by Risk Service: {}", riskResult.rejectionReason());
            throw new IllegalArgumentException("Transaction Rejected: " + riskResult.rejectionReason());
        }

        account.withdraw(command.amount());
        saveAccountPort.saveAccount(account);
        
        finalizeTransaction(command.accountId(), "WITHDRAW", command.amount());
    }

    private void executeTransaction(UUID accountId, Consumer<Account> action) {
        Account account = getAccountOrThrow(accountId);
        action.accept(account);
        saveAccountPort.saveAccount(account);
    }

    private void finalizeTransaction(UUID accountId, String type, BigDecimal amount) {
        AccountTransactionEvent event = new AccountTransactionEvent(
            UUID.randomUUID(), accountId, type, amount, LocalDateTime.now()
        );
        publishTransactionPort.publish(event);
        
        log.info("{} Successful. Amount: {}", type, amount);
    }

    private Account getAccountOrThrow(UUID accountId) {
        return loadAccountPort.loadAccount(accountId).orElseThrow(() -> {
            log.error("Transaction Failed: Account {} not found", accountId);
            return new NoSuchElementException("Account not found with ID: " + accountId);
        });
    }
}