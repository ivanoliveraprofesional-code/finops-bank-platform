package com.finops.bank.auth.domain.exception;

public class KeyInitializationException extends RuntimeException {
    public KeyInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}