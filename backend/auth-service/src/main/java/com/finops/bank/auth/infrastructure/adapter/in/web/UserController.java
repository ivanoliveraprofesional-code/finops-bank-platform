package com.finops.bank.auth.infrastructure.adapter.in.web;

import com.finops.bank.auth.application.port.in.ManageUsersUseCase;
import com.finops.bank.auth.application.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.finops.bank.auth.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ManageUsersUseCase manageUsersUseCase;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(manageUsersUseCase.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        return ResponseEntity.ok(manageUsersUseCase.getUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(
            @PathVariable String id, 
            @RequestBody UpdateUserRequest request) {
        
        UpdateUserCommand command = new UpdateUserCommand(
            id, request.email(), request.roles(), request.password()
        );
        manageUsersUseCase.updateUser(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        manageUsersUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    public record UpdateUserRequest(String email, String roles, String password) {}
}