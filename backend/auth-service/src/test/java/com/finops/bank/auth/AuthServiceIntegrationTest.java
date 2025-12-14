package com.finops.bank.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users, user_roles CASCADE");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterUser() throws Exception {
        Map<String, String> request = Map.of(
            "username", "newuser",
            "password", "password123",
            "email", "new@test.com"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("newuser")));
    }

    @Test
    @DisplayName("Should fail registration with duplicate username")
    void shouldFailDuplicateRegister() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("username", "dup", "password", "pass1234", "email", "dup@test.com"));
        
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Data Conflict")));
    }

    @Test
    @DisplayName("Should login successfully and return token")
    void shouldLogin() throws Exception {
        String regJson = objectMapper.writeValueAsString(Map.of("username", "loginuser", "password", "securepass", "email", "login@test.com"));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(regJson))
                .andExpect(status().isOk());

        Map<String, String> loginRequest = Map.of("username", "loginuser", "password", "securepass");
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginBadCreds() throws Exception {
        String regJson = objectMapper.writeValueAsString(Map.of(
            "username", "wrongpass", 
            "password", "correctPass", 
            "email", "wp@test.com"
        ));
        
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(regJson))
                .andExpect(status().isOk());

        Map<String, String> loginRequest = Map.of("username", "wrongpass", "password", "WRONG_PASSWORD");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update user fields")
    void shouldUpdateUser() throws Exception {
        String regJson = objectMapper.writeValueAsString(Map.of("username", "toupdate", "password", "pass1234", "email", "old@test.com"));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(regJson))
                .andExpect(status().isOk());
        
        String response = mockMvc.perform(get("/api/users"))
                .andReturn().getResponse().getContentAsString();
        
        String id = objectMapper.readTree(response).get(0).get("id").asText();

        Map<String, String> updateReq = Map.of("email", "updated@test.com", "roles", "ROLE_ADMIN");
        mockMvc.perform(put("/api/users/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("updated@test.com")))
                .andExpect(jsonPath("$.roles", containsString("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() throws Exception {
        String regJson = objectMapper.writeValueAsString(Map.of("username", "todelete", "password", "pass1234", "email", "del@test.com"));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(regJson))
                .andExpect(status().isOk());
        
        String response = mockMvc.perform(get("/api/users")).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get(0).get("id").asText();

        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isNotFound());
    }
}