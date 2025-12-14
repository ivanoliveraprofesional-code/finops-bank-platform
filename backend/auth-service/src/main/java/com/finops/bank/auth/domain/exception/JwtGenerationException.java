package com.finops.bank.auth.domain.exception;

public class JwtGenerationException extends RuntimeException {
	private static final long serialVersionUID = 215219607000643692L;

	public JwtGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}