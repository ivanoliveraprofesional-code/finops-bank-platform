package com.finops.bank.auth.application.port.out;

import com.finops.bank.auth.domain.model.User;

public interface SaveUserPort {
    void save(User user);
    boolean existsByUsername(String username);
    void deleteUser(String id);
}