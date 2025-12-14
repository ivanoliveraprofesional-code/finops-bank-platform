package com.finops.bank.core.infrastructure.adapter.out.messaging;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.finops.bank.core.application.port.out.PublishTransactionPort;
import com.finops.bank.core.domain.event.AccountTransactionEvent;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SqsEventPublisher implements PublishTransactionPort {

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public SqsEventPublisher(SqsTemplate sqsTemplate, 
                             @Value("${app.messaging.queue-name}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = queueName;
    }

    @Override
    public void publish(AccountTransactionEvent event) {
        String traceId = MDC.get("traceId");
        log.info("Publishing Transaction Event to SQS [{}]: {}", queueName, event);
        
        sqsTemplate.send(to -> to
            .queue(queueName)
            .payload(event)
            .header("traceId", traceId != null ? traceId : "") 
        );
    }
}