package com.finops.bank.auth.application.port.in;

public interface LoginUseCase {
    String login(LoginCommand command);

    record LoginCommand(String username, String password) {
        public LoginCommand {
            if (username == null || username.isBlank()) throw new IllegalArgumentException("Username required");
            if (password == null || password.isBlank()) throw new IllegalArgumentException("Password required");
        }
    }
}