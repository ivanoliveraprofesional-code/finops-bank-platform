package com.finops.bank.auth.infrastructure.persistence;

import com.finops.bank.auth.domain.model.User;
import com.finops.bank.auth.infrastructure.persistence.entity.UserEntity;
import com.finops.bank.auth.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private SpringDataUserRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldLoadUserByUsername() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity(userId, "testuser", "hashedpass", "test@email.com", Set.of("ROLE_USER"));
        repository.save(entity);

        Optional<User> result = adapter.loadUserByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@email.com");
        assertThat(result.get().getRoles()).contains("ROLE_USER");
    }

    @Test
    void shouldLoadUserById() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity(userId, "iduser", "pass", "id@email.com", Set.of("ROLE_ADMIN"));
        repository.save(entity);

        Optional<User> result = adapter.loadUserById(userId.toString());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId.toString());
        assertThat(result.get().getUsername()).isEqualTo("iduser");
    }

    @Test
    void shouldLoadAllUsers() {
        repository.save(new UserEntity(UUID.randomUUID(), "user1", "pass", "u1@test.com", Set.of("ROLE_USER")));
        repository.save(new UserEntity(UUID.randomUUID(), "user2", "pass", "u2@test.com", Set.of("ROLE_ADMIN")));

        List<User> users = adapter.loadAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void shouldSaveNewUser() {
        String id = UUID.randomUUID().toString();
        User domainUser = new User(id, "newuser", "securehash", "new@test.com", "ROLE_USER,ROLE_MANAGER");

        adapter.save(domainUser);

        Optional<UserEntity> savedEntity = repository.findById(UUID.fromString(id));
        assertThat(savedEntity).isPresent();
        assertThat(savedEntity.get().getUsername()).isEqualTo("newuser");
        assertThat(savedEntity.get().getEmail()).isEqualTo("new@test.com");
        assertThat(savedEntity.get().getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
    }

    @Test
    void shouldUpdateExistingUser() {
        UUID id = UUID.randomUUID();
        UserEntity original = new UserEntity(id, "updateuser", "oldpass", "old@test.com", Set.of("ROLE_USER"));
        repository.save(original);

        User updatedDomain = new User(id.toString(), "updateuser", "NEW_PASS", "new@test.com", "ROLE_ADMIN");

        adapter.save(updatedDomain);

        UserEntity fromDb = repository.findById(id).get();
        assertThat(fromDb.getPasswordHash()).isEqualTo("NEW_PASS");
        assertThat(fromDb.getEmail()).isEqualTo("new@test.com");
        assertThat(fromDb.getRoles()).containsOnly("ROLE_ADMIN");
    }

    @Test
    void shouldDeleteUser() {
        UUID id = UUID.randomUUID();
        repository.save(new UserEntity(id, "delete-me", "pass", "del@test.com", Set.of("ROLE_USER")));

        adapter.deleteUser(id.toString());

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> result = adapter.loadUserByUsername("ghost");
        assertThat(result).isEmpty();

        Optional<User> idResult = adapter.loadUserById(UUID.randomUUID().toString());
        assertThat(idResult).isEmpty();
    }
}