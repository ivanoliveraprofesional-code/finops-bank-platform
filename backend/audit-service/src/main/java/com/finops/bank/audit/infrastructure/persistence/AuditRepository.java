package com.finops.bank.audit.infrastructure.persistence;

import com.finops.bank.audit.domain.model.AuditLog;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

@Repository
public class AuditRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    
    private DynamoDbTable<AuditLog> auditTable;

    public AuditRepository(DynamoDbEnhancedClient enhancedClient,
                           @Value("${app.database.table-name:finops-audit-logs}") String tableName) {
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
    }

    @PostConstruct
    public void init() {
        this.auditTable = enhancedClient.table(tableName, TableSchema.fromBean(AuditLog.class));
    }

    public void save(AuditLog log) {
        auditTable.putItem(log);
    }
}