package com.finops.bank.core.application.port.out;

import com.finops.bank.core.domain.model.Account;
import java.util.Optional;
import java.util.UUID;

public interface LoadAccountPort {
    Optional<Account> loadAccount(UUID id);
}