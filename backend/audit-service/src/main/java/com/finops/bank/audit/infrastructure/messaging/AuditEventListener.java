package com.finops.bank.audit.infrastructure.messaging;

import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.finops.bank.audit.domain.exception.AuditProcessingException;
import com.finops.bank.audit.domain.model.AuditLog;
import com.finops.bank.audit.infrastructure.messaging.dto.TransactionEventDto;
import com.finops.bank.audit.infrastructure.persistence.AuditRepository;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditRepository auditRepository;

    @SqsListener("${app.messaging.queue-name}")
    public void receiveMessage(@Payload TransactionEventDto event, 
                               @Header(value = "traceId", required = false) String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put("traceId", traceId);
        }

        try {
            log.info("Received Audit Event: {}", event);
            AuditLog auditLog = mapToDomain(event);
            auditRepository.save(auditLog);
            log.info("Audit Log saved. ID: {}", event.eventId());
        } catch (Exception e) {
            log.error("Error processing audit event: {}", event.eventId(), e);
            throw new AuditProcessingException("Failed to persist audit log", e);
        } finally {
            MDC.remove("traceId");
        }
    }
    
    private AuditLog mapToDomain(TransactionEventDto event) {
        return new AuditLog(
            event.eventId().toString(),
            event.timestamp().toString(),
            event.accountId().toString(),
            event.type(),
            event.amount().toString()
        );
    }
}