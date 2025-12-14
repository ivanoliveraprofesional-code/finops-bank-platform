package com.finops.bank.audit.domain.exception;

public class AuditProcessingException extends RuntimeException {
	private static final long serialVersionUID = -5204132088363895683L;

	public AuditProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}