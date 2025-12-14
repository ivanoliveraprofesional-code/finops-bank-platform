package com.finops.bank.core.application.service;

import com.finops.bank.core.application.port.in.GetAccountUseCase.AccountResponse;
import com.finops.bank.core.application.port.out.LoadAccountPort;
import com.finops.bank.core.domain.model.Account;
import com.finops.bank.core.domain.model.AccountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock
    private LoadAccountPort loadAccountPort;

    @InjectMocks
    private AccountQueryService accountQueryService;

    @Test
    void shouldReturnAccount_WhenAccountExists() {
        UUID accountId = UUID.randomUUID();
        Account mockAccount = new Account(
            accountId, UUID.randomUUID(), BigDecimal.TEN, 
            AccountStatus.ACTIVE, "USD", LocalDateTime.now()
        );

        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.of(mockAccount));

        AccountResponse response = accountQueryService.getAccount(accountId);

        assertEquals(accountId, response.id());
        assertEquals(BigDecimal.TEN, response.balance());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void shouldThrowException_WhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(loadAccountPort.loadAccount(accountId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> accountQueryService.getAccount(accountId));
    }
}