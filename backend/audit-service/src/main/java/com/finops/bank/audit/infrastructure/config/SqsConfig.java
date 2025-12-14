package com.finops.bank.audit.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {

    @Bean
    public SqsMessagingMessageConverter sqsMessagingMessageConverter(ObjectMapper objectMapper) {
        SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();

        converter.setObjectMapper(objectMapper);
        converter.setPayloadTypeHeader("ignore-me");

        return converter;
    }

    @Bean
    @Primary
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient,
            SqsMessagingMessageConverter messageConverter) {

        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.messageConverter(messageConverter))
                .build();
    }
}
