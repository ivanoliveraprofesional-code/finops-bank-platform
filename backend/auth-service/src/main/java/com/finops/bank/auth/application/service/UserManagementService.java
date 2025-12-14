package com.finops.bank.auth.application.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finops.bank.auth.application.port.in.ManageUsersUseCase;
import com.finops.bank.auth.application.port.out.LoadUserPort;
import com.finops.bank.auth.application.port.out.SaveUserPort;
import com.finops.bank.auth.domain.exception.UserNotFoundException;
import com.finops.bank.auth.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService implements ManageUsersUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getUser(String id) {
        return loadUserPort.loadUserById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Override
    public List<User> getAllUsers() {
        return loadUserPort.loadAllUsers();
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserCommand command) {
        User user = getUser(command.id());

        if (command.email() != null && !command.email().isBlank()) {
            user.setEmail(command.email());
        }
        
        if (command.roles() != null && !command.roles().isBlank()) {
            user.setRoles(command.roles());
        }

        if (command.password() != null && !command.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(command.password()));
        }

        saveUserPort.save(user);
        log.info("User updated: {}", user.getId());
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        if (loadUserPort.loadUserById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        saveUserPort.deleteUser(id);
        log.info("User deleted: {}", id);
    }
}