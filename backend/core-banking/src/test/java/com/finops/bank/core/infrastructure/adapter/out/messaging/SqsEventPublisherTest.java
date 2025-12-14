package com.finops.bank.core.infrastructure.adapter.out.messaging;

import com.finops.bank.core.application.service.AccountQueryService;
import com.finops.bank.core.application.service.AccountService;
import com.finops.bank.core.domain.event.AccountTransactionEvent;
import com.finops.bank.core.infrastructure.adapter.out.persistence.repository.SpringDataAccountRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = SqsEventPublisherTest.LocalStackInitializer.class)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
})
class SqsEventPublisherTest {

    private static final String QUEUE_NAME = "finops-audit-queue";

    @SuppressWarnings("resource")
	@Container
    static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.2")
    ).withServices(SQS);

    static class LocalStackInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            if (!localStack.isRunning()) {
                localStack.start();
            }

            try (SqsClient sqsClient = SqsClient.builder()
                    .endpointOverride(localStack.getEndpointOverride(SQS))
                    .region(Region.of(localStack.getRegion()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
                            )
                    )
                    .build()) {

                String queueUrl = sqsClient.createQueue(
                        CreateQueueRequest.builder()
                                .queueName(QUEUE_NAME)
                                .build()
                ).queueUrl();

                System.setProperty("spring.cloud.aws.region.static", localStack.getRegion());
                System.setProperty("spring.cloud.aws.credentials.access-key", localStack.getAccessKey());
                System.setProperty("spring.cloud.aws.credentials.secret-key", localStack.getSecretKey());
                System.setProperty("spring.cloud.aws.sqs.endpoint", localStack.getEndpointOverride(SQS).toString());

                System.setProperty("app.messaging.queue-name", queueUrl);
            }
        }
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
