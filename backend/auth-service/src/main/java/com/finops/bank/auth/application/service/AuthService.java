package com.finops.bank.auth.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finops.bank.auth.application.port.in.LoginUseCase;
import com.finops.bank.auth.application.port.in.RegisterUseCase;
import com.finops.bank.auth.application.port.out.LoadUserPort;
import com.finops.bank.auth.application.port.out.SaveUserPort;
import com.finops.bank.auth.application.port.out.TokenGeneratorPort;
import com.finops.bank.auth.domain.exception.UserAlreadyExistsException;
import com.finops.bank.auth.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements LoginUseCase, RegisterUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String login(LoginCommand command) {
        log.info("Attempting login for user: {}", command.username());

        User user = loadUserPort.loadUserByUsername(command.username())
                .orElseThrow(() -> new SecurityException("Invalid Credentials"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new SecurityException("Invalid Credentials");
        }

        String token = tokenGeneratorPort.generateToken(user);
        log.info("Login successful for user: {}", user.getId());
        return token;
    }
    
    @Override
    @Transactional
    public void register(RegisterCommand command) {
        if (saveUserPort.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User newUser = new User();
        newUser.setId(UUID.randomUUID().toString());
        newUser.setUsername(command.username());
        newUser.setPasswordHash(passwordEncoder.encode(command.password()));
        newUser.setEmail(command.email());
        newUser.setRoles("ROLE_USER");

        saveUserPort.save(newUser);
        log.info("User registered successfully: {}", command.username());
    }
}