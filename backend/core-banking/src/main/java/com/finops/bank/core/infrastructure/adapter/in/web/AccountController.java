package com.finops.bank.core.infrastructure.adapter.in.web;

import com.finops.bank.core.application.port.in.CreateAccountUseCase;
import com.finops.bank.core.application.port.in.CreateAccountUseCase.CreateAccountCommand;
import com.finops.bank.core.application.port.in.GetAccountUseCase;
import com.finops.bank.core.application.port.in.GetAccountUseCase.AccountResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase, 
                             GetAccountUseCase getAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
    }

    @PostMapping
    public ResponseEntity<UUID> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        CreateAccountCommand command = new CreateAccountCommand(request.userId(), request.currency());
        UUID accountId = createAccountUseCase.createAccount(command);
        return ResponseEntity.ok(accountId);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(getAccountUseCase.getAccount(id));
    }

    public record CreateAccountRequest(@NotNull UUID userId, @NotNull String currency) {}
}