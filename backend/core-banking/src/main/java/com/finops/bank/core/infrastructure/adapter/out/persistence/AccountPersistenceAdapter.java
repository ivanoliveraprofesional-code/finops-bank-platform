package com.finops.bank.core.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.finops.bank.core.application.port.out.LoadAccountPort;
import com.finops.bank.core.application.port.out.SaveAccountPort;
import com.finops.bank.core.domain.model.Account;
import com.finops.bank.core.domain.model.AccountStatus;
import com.finops.bank.core.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.finops.bank.core.infrastructure.adapter.out.persistence.repository.SpringDataAccountRepository;

@Component
public class AccountPersistenceAdapter implements SaveAccountPort, LoadAccountPort {

    private final SpringDataAccountRepository repository;

    public AccountPersistenceAdapter(SpringDataAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAccount(Account account) {
        AccountEntity entity = new AccountEntity(
            account.getId(),
            account.getUserId(),
            account.getBalance(),
            account.getStatus().name(),
            account.getCurrency(),
            account.getCreatedAt()
        );
        
        repository.save(entity);
    }
    
    @Override
    public Optional<Account> loadAccount(UUID id) {
        return repository.findById(id)
                .map(entity -> new Account(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getBalance(),
                        AccountStatus.valueOf(entity.getStatus()),
                        entity.getCurrency(),
                        entity.getCreatedAt()
                ));
    }
}