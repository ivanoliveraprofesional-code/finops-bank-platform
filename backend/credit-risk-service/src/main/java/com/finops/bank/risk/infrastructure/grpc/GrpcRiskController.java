package com.finops.bank.risk.infrastructure.grpc;

import java.math.BigDecimal;
import java.util.UUID;

import com.finops.bank.risk.application.port.in.AssessRiskUseCase;
import com.finops.bank.risk.application.port.in.AssessRiskUseCase.AssessCommand;
import com.finops.bank.risk.domain.model.RiskAssessment;
import com.finops.bank.risk.generated.CreditRiskRequest;
import com.finops.bank.risk.generated.CreditRiskResponse;
import com.finops.bank.risk.generated.RiskAssessmentServiceGrpc;
import com.finops.bank.risk.generated.RiskLevel;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcRiskController extends RiskAssessmentServiceGrpc.RiskAssessmentServiceImplBase {

    private final AssessRiskUseCase assessRiskUseCase;

    @Override
    public void assessCreditRisk(CreditRiskRequest request, StreamObserver<CreditRiskResponse> responseObserver) {
        try {
            AssessCommand command = new AssessCommand(
                    UUID.fromString(request.getUserId()),
                    BigDecimal.valueOf(request.getAmount())
            );

            RiskAssessment assessment = assessRiskUseCase.assess(command);

            CreditRiskResponse response = CreditRiskResponse.newBuilder()
                    .setIsApproved(assessment.isApproved())
                    .setRejectionReason(assessment.getRejectionReason() == null ? "" : assessment.getRejectionReason())
                    .setRiskLevel(mapRiskLevel(assessment.getRiskLevel())) 
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    private RiskLevel mapRiskLevel(RiskAssessment.RiskLevel domainLevel) {
        if (domainLevel == null) return RiskLevel.LOW;
        
        return switch (domainLevel) {
            case LOW -> RiskLevel.LOW;
            case MEDIUM -> RiskLevel.MEDIUM;
            case HIGH -> RiskLevel.HIGH;
            case CRITICAL -> RiskLevel.CRITICAL;
        };
    }
}