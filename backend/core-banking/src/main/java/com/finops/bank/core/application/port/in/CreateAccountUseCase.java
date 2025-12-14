package com.finops.bank.core.application.port.in;

import java.util.UUID;

public interface CreateAccountUseCase {
 UUID createAccount(CreateAccountCommand command);

 record CreateAccountCommand(UUID userId, String currency) {
     public CreateAccountCommand {
         if (userId == null) throw new IllegalArgumentException("UserId cannot be null");
         if (currency == null || currency.isBlank()) throw new IllegalArgumentException("Currency is required");
     }
 }
}