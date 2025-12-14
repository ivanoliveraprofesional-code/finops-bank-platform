package com.finops.bank.core.application.port.out;

import com.finops.bank.core.domain.model.Account;

public interface SaveAccountPort {
    void saveAccount(Account account);
}