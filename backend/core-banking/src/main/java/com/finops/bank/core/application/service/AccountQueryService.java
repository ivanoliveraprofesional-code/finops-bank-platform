package com.finops.bank.core.application.service;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finops.bank.core.application.port.in.GetAccountUseCase;
import com.finops.bank.core.application.port.out.LoadAccountPort;
import com.finops.bank.core.domain.model.Account;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountQueryService implements GetAccountUseCase {

    private final LoadAccountPort loadAccountPort;

    @Override
    public AccountResponse getAccount(UUID id) {
        Account account = loadAccountPort.loadAccount(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found with ID: " + id));

        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus().name()
        );
    }
}