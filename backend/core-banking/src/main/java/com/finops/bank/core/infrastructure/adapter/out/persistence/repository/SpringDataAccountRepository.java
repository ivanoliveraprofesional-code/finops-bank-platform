package com.finops.bank.core.infrastructure.adapter.out.persistence.repository;

import com.finops.bank.core.infrastructure.adapter.out.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, UUID> {}