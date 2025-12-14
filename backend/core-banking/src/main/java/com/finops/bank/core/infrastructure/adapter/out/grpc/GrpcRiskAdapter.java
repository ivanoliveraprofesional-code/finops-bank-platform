package com.finops.bank.core.infrastructure.adapter.out.grpc;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.finops.bank.core.application.port.out.RiskCheckPort;
import com.finops.bank.core.domain.exception.RiskServiceUnavailableException;
import com.finops.bank.risk.generated.CreditRiskRequest;
import com.finops.bank.risk.generated.CreditRiskResponse;
import com.finops.bank.risk.generated.RiskAssessmentServiceGrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Component
public class GrpcRiskAdapter implements RiskCheckPort {

    @GrpcClient("credit-risk-service")
    private RiskAssessmentServiceGrpc.RiskAssessmentServiceBlockingStub riskStub;

    @Override
    public RiskCheckResult checkRisk(UUID userId, BigDecimal amount) {
        log.info("Calling gRPC Credit Risk for User: {}", userId);

        try {
            CreditRiskRequest request = CreditRiskRequest.newBuilder()
                    .setUserId(userId.toString())
                    .setAmount(amount.doubleValue())
                    .build();

            CreditRiskResponse response = riskStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .assessCreditRisk(request);

            return new RiskCheckResult(
                    response.getIsApproved(),
                    response.getRejectionReason()
            );

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                log.error("Risk Service timed out (Deadline Exceeded)");
            } else if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                log.error("Risk Service is unreachable (Connection failed)");
            }
            throw new RiskServiceUnavailableException("Risk Check Failed: " + e.getStatus().getCode(), e);
            
        } catch (Exception e) {
            log.error("Unexpected error in gRPC call", e);
            throw new RiskServiceUnavailableException("Risk Check Service Unavailable", e);
        }
    }
}