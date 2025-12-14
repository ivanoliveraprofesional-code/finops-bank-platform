package com.finops.bank.audit.domain.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class AuditLog {
    
    private String transactionId;
    private String timestamp;
    private String accountId;
    private String type;
    private String amount;

    public AuditLog() {}

    public AuditLog(String transactionId, String timestamp, String accountId, String type, String amount) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
    }

    @DynamoDbPartitionKey
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @DynamoDbSortKey
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
}