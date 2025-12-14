package com.finops.bank.core.application.port.out;

import com.finops.bank.core.domain.event.AccountTransactionEvent;

public interface PublishTransactionPort {
    void publish(AccountTransactionEvent event);
}