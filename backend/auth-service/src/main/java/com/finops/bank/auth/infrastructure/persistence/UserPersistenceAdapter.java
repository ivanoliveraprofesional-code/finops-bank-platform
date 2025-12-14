package com.finops.bank.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.finops.bank.auth.application.port.out.LoadUserPort;
import com.finops.bank.auth.application.port.out.SaveUserPort;
import com.finops.bank.auth.domain.model.User;
import com.finops.bank.auth.infrastructure.persistence.entity.UserEntity;
import com.finops.bank.auth.infrastructure.persistence.repository.SpringDataUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final SpringDataUserRepository repository;

    @Override
    public Optional<User> loadUserByUsername(String username) {
        return repository.findByUsername(username).map(this::mapToDomain);
    }

    @Override
    public Optional<User> loadUserById(String id) {
        return repository.findById(UUID.fromString(id)).map(this::mapToDomain);
    }

    @Override
    public List<User> loadAllUsers() {
        return repository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void save(User user) {
        UserEntity entity = mapToEntity(user);
        repository.save(entity);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.findByUsername(username).isPresent();
    }

    @Override
    public void deleteUser(String id) {
        repository.deleteById(UUID.fromString(id));
    }

    private User mapToDomain(UserEntity entity) {
        String rolesString = entity.getRoles() != null 
            ? String.join(",", entity.getRoles()) 
            : "";
            
        return new User(
                entity.getId().toString(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getEmail(),
                rolesString
        );
    }

    private UserEntity mapToEntity(User user) {
        Set<String> rolesSet = user.getRoles() != null && !user.getRoles().isBlank()
                ? Set.of(user.getRoles().split(","))
                : Set.of();

        return new UserEntity(
                UUID.fromString(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                user.getEmail(),
                rolesSet
        );
    }
}