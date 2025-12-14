package com.finops.bank.auth.infrastructure.adapter.in.web;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.finops.bank.auth.domain.exception.JwtGenerationException;
import com.finops.bank.auth.domain.exception.KeyInitializationException;
import com.finops.bank.auth.domain.exception.UserAlreadyExistsException;
import com.finops.bank.auth.domain.exception.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message) {}

    @ExceptionHandler({SecurityException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthFailure(RuntimeException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid username or password");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource Not Found", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Request", e.getMessage());
    }

    @ExceptionHandler({JwtGenerationException.class, KeyInitializationException.class})
    public ResponseEntity<ErrorResponse> handleCriticalInfraError(RuntimeException e) {
        e.printStackTrace(); 
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Infrastructure Error", "Security system failure. Please contact support.");
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException e) {
        return buildResponse(HttpStatus.CONFLICT, "Data Conflict", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        return buildResponse(HttpStatus.CONFLICT, "Data Conflict", "Duplicate username or email.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("details", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        e.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(), error, message));
    }    
}