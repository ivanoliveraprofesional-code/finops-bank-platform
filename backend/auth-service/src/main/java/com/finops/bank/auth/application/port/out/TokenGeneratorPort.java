package com.finops.bank.auth.application.port.out;

import com.finops.bank.auth.domain.model.User;

public interface TokenGeneratorPort {
    String generateToken(User user);
} 