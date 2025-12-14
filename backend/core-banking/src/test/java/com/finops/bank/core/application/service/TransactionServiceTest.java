package com.finops.bank.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finops.bank.core.application.port.in.DepositUseCase.DepositCommand;
import com.finops.bank.core.application.port.in.WithdrawUseCase.WithdrawCommand;
import com.finops.bank.core.application.port.out.LoadAccountPort;
import com.finops.bank.core.application.port.out.PublishTransactionPort;
import com.finops.bank.core.application.port.out.RiskCheckPort;
import com.finops.bank.core.application.port.out.RiskCheckPort.RiskCheckResult;
import com.finops.bank.core.application.port.out.SaveAccountPort;
import com.finops.bank.core.domain.exception.InsufficientFundsException;
import com.finops.bank.core.domain.exception.RiskServiceUnavailableException;
import com.finops.bank.core.domain.model.Account;
import com.finops.bank.core.domain.model.AccountStatus;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private LoadAccountPort loadAccountPort;
    @Mock private SaveAccountPort saveAccountPort;
    @Mock private PublishTransactionPort publishTransactionPort;
    @Mock private RiskCheckPort riskCheckPort;
    
    @InjectMocks private TransactionService transactionService;

    private Account mockAccount;
    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockAccount = new Account(
            accountId, 
            userId, 
            new BigDecimal("0.00"), 
            AccountStatus.ACTIVE, 
            "USD", 
            LocalDateTime.now()
        );
    }

    @Test
    void depositSuccess() {
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));
        DepositCommand command = new DepositCommand(accountId, new BigDecimal("100.00"));

        transactionService.deposit(command);

        verify(saveAccountPort).saveAccount(any(Account.class));
    }

    @Test
    @DisplayName("Withdraw: Should decrease balance when Risk Approved")
    void withdrawSuccess() {
        mockAccount.deposit(new BigDecimal("200.00"));
        
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));
        
        when(riskCheckPort.checkRisk(userId, new BigDecimal("50.00")))
            .thenReturn(new RiskCheckResult(true, null));

        WithdrawCommand command = new WithdrawCommand(accountId, new BigDecimal("50.00"));

        transactionService.withdraw(command);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(saveAccountPort).saveAccount(captor.capture());
        
        assertEquals(new BigDecimal("150.00"), captor.getValue().getBalance());
    }

    @Test
    @DisplayName("Withdraw: Should Fail when Risk Rejected")
    void withdrawFailure_RiskRejected() {
        mockAccount.deposit(new BigDecimal("200.00"));
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));

        when(riskCheckPort.checkRisk(userId, new BigDecimal("50.00")))
            .thenReturn(new RiskCheckResult(false, "High Risk Score"));

        WithdrawCommand command = new WithdrawCommand(accountId, new BigDecimal("50.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> transactionService.withdraw(command));
            
        assertEquals("Transaction Rejected: High Risk Score", ex.getMessage());
        
        verify(saveAccountPort, never()).saveAccount(any());
    }
    
    @Test
    @DisplayName("Withdraw: Should Fail Fast when Risk Service is Down")
    void withdrawFailure_RiskServiceDown() {
        mockAccount.deposit(new BigDecimal("200.00"));
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));

        when(riskCheckPort.checkRisk(any(), any()))
            .thenThrow(new RiskServiceUnavailableException("Risk Check Failed Fast: DEADLINE_EXCEEDED", new RuntimeException()));

        WithdrawCommand command = new WithdrawCommand(accountId, new BigDecimal("50.00"));

        assertThrows(RiskServiceUnavailableException.class, 
            () -> transactionService.withdraw(command));
            
        verify(saveAccountPort, never()).saveAccount(any());
    }   

    @Test
    @DisplayName("Withdraw: Should fail on Insufficient Funds (Risk check skipped or irrelevant)")
    void withdrawFailure_InsufficientFunds() {
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));
        
        when(riskCheckPort.checkRisk(userId, new BigDecimal("50.00")))
            .thenReturn(new RiskCheckResult(true, null));

        WithdrawCommand command = new WithdrawCommand(accountId, new BigDecimal("50.00"));

        assertThrows(InsufficientFundsException.class, () -> transactionService.withdraw(command));
        verify(saveAccountPort, never()).saveAccount(any());
    }

    @Test
    void accountNotFound() {
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.empty());
        DepositCommand command = new DepositCommand(accountId, BigDecimal.TEN);

        assertThrows(NoSuchElementException.class, () -> transactionService.deposit(command));
    }
}