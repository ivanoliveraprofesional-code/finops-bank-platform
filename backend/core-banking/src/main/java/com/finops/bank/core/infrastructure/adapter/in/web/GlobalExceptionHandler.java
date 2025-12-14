package com.finops.bank.core.infrastructure.adapter.in.web;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.finops.bank.core.domain.exception.InsufficientFundsException;
import com.finops.bank.core.domain.exception.RiskServiceUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
    ) {}

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException e) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Transaction Failed", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Request", e.getMessage());
    }

    @ExceptionHandler(RiskServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleRiskServiceDown(RiskServiceUnavailableException e) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Dependency Failure", "Risk assessment service is temporarily unavailable.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        e.printStackTrace(); 
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String errorType, String message) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            errorType,
            message
        );
        return ResponseEntity.status(status).body(error);
    }
}