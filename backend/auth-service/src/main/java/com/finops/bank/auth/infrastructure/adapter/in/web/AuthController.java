package com.finops.bank.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finops.bank.auth.application.port.in.LoginUseCase;
import com.finops.bank.auth.application.port.in.LoginUseCase.LoginCommand;
import com.finops.bank.auth.application.port.in.RegisterUseCase;
import com.finops.bank.auth.application.port.in.RegisterUseCase.RegisterCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginCommand command = new LoginCommand(request.username(), request.password());
        String token = loginUseCase.login(command);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(request.username(), request.password(), request.email());
        registerUseCase.register(command);
        return ResponseEntity.ok().build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String username, @NotBlank String password, String email) {}
    public record TokenResponse(String token) {}
}