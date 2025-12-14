package com.finops.bank.audit.infrastructure.messaging;

import com.finops.bank.audit.domain.model.AuditLog;
import com.finops.bank.audit.infrastructure.messaging.dto.TransactionEventDto;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
class AuditIntegrationTest {

    private static final String QUEUE_NAME = "finops-audit-queue";
    private static final String TABLE_NAME = "finops-audit-logs";

    @SuppressWarnings("resource")
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.2")
    ).withServices(SQS, DYNAMODB);

    static {
        localStack.start();
    }

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS));
        registry.add("spring.cloud.aws.dynamodb.endpoint", () -> localStack.getEndpointOverride(DYNAMODB));
        registry.add("app.messaging.queue-name", () -> QUEUE_NAME);
        registry.add("app.database.table-name", () -> TABLE_NAME);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        createInfrastructure();
    }

    @Test
    void shouldConsumeMessageAndSaveToDynamoDB() {
        UUID eventId = UUID.randomUUID();
        
        TransactionEventDto event = new TransactionEventDto(
            eventId,
            UUID.randomUUID(),
            "DEPOSIT",
            new BigDecimal("500.00"),
            LocalDateTime.now()
        );

        sqsTemplate.send(to -> to.queue(QUEUE_NAME).payload(event));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(AuditLog.class));
            
            AuditLog savedLog = table.getItem(Key.builder()
                .partitionValue(eventId.toString())
                .sortValue(event.timestamp().toString())
                .build());
            
            assertThat(savedLog).isNotNull();
            assertThat(savedLog.getAmount()).isEqualTo("500.00");
            assertThat(savedLog.getType()).isEqualTo("DEPOSIT");
        });
    }

    private static void createInfrastructure() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
        
        localStack.execInContainer("awslocal", "dynamodb", "create-table",
                "--table-name", TABLE_NAME,
                "--attribute-definitions", 
                    "AttributeName=transactionId,AttributeType=S", 
                    "AttributeName=timestamp,AttributeType=S",
                "--key-schema", 
                    "AttributeName=transactionId,KeyType=HASH", 
                    "AttributeName=timestamp,KeyType=RANGE",
                "--billing-mode", "PAY_PER_REQUEST",
                "--region", localStack.getRegion());
    }
}