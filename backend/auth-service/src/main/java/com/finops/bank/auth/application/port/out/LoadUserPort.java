package com.finops.bank.auth.application.port.out;

import java.util.List;
import java.util.Optional;
import com.finops.bank.auth.domain.model.User;

public interface LoadUserPort {
    Optional<User> loadUserByUsername(String username);
    Optional<User> loadUserById(String id);
    List<User> loadAllUsers();
}