package com.finops.bank.auth.application.port.in;

public interface RegisterUseCase {
    void register(RegisterCommand command);

    record RegisterCommand(String username, String password, String email) {
        public RegisterCommand {
            if (username == null || username.isBlank()) throw new IllegalArgumentException("Username required");
            if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 chars");
        }
    }
}