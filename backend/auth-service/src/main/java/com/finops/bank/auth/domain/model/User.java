package com.finops.bank.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String email;

    @Builder.Default
    private String roles = "ROLE_USER"; 
}