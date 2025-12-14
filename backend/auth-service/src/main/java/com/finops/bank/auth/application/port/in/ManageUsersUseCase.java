package com.finops.bank.auth.application.port.in;

import com.finops.bank.auth.domain.model.User;
import java.util.List;

public interface ManageUsersUseCase {
    User getUser(String id);
    List<User> getAllUsers();
    void updateUser(UpdateUserCommand command);
    void deleteUser(String id);

    record UpdateUserCommand(String id, String email, String roles, String password) {
        public UpdateUserCommand {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("ID required");
        }
    }
}