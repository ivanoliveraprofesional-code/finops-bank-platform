package com.finops.bank.core.infrastructure.adapter.out.messaging;

import com.finops.bank.core.application.service.AccountQueryService;
import com.finops.bank.core.application.service.AccountService;
import com.finops.bank.core.domain.event.AccountTransactionEvent;
import com.finops.bank.core.infrastructure.adapter.out.persistence.repository.SpringDataAccountRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
class SqsEventPublisherTest {

    private static final String QUEUE_NAME = "finops-audit-queue";

    @SuppressWarnings("resource")
	static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.2")
    ).withServices(SQS);

    static {
        localStack.start();
    }

    @Autowired
    private SqsEventPublisher publisher;

    @Autowired
    private SqsTemplate sqsTemplate;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountQueryService accountQueryService;

    @MockBean
    private SpringDataAccountRepository springDataAccountRepository;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS));
        registry.add("app.messaging.queue-name", () -> QUEUE_NAME);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
    }

    @Test
    void shouldPublishEventToSqs() {
        AccountTransactionEvent event = new AccountTransactionEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "DEPOSIT",
            BigDecimal.TEN,
            LocalDateTime.now()
        );

        publisher.publish(event);

        var received = sqsTemplate.receive(
            from -> from.queue(QUEUE_NAME),
            AccountTransactionEvent.class
        );

        assertThat(received).isPresent();
        assertThat(received.get().getPayload().type()).isEqualTo("DEPOSIT");
        assertThat(received.get().getPayload().amount()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
