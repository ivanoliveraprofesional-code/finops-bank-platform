package com.finops.bank.core.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finops.bank.core.domain.model.Account;
import com.finops.bank.core.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.finops.bank.core.infrastructure.adapter.out.persistence.repository.SpringDataAccountRepository;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountPersistenceAdapter.class)
class AccountPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @Autowired
    private AccountPersistenceAdapter adapter;

    @Autowired
    private SpringDataAccountRepository repository;

    @Test
    void shouldSaveAndLoadAccount() {
        Account account = new Account(UUID.randomUUID(), "USD");
        
        adapter.saveAccount(account);

        Optional<AccountEntity> savedEntity = repository.findById(account.getId());
        assertThat(savedEntity).isPresent();
        assertThat(savedEntity.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        Optional<Account> loadedAccount = adapter.loadAccount(account.getId());
        assertThat(loadedAccount).isPresent();
        assertThat(loadedAccount.get().getId()).isEqualTo(account.getId());
    }
}