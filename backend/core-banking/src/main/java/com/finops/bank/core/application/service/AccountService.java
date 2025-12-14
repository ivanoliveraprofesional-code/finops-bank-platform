package com.finops.bank.core.application.service;

import com.finops.bank.core.application.port.in.CreateAccountUseCase;
import com.finops.bank.core.application.port.out.SaveAccountPort;
import com.finops.bank.core.domain.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountService implements CreateAccountUseCase {

    private final SaveAccountPort saveAccountPort;

    @Override
    public UUID createAccount(CreateAccountCommand command) {
        log.info("Creating new account for User: {} with Currency: {}", command.userId(), command.currency());
        
        Account newAccount = new Account(command.userId(), command.currency());
        saveAccountPort.saveAccount(newAccount);
        
        log.info("Account created successfully. Account ID: {}", newAccount.getId());
        return newAccount.getId();
    }
}