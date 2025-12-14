package com.finops.bank.core.domain.exception;

public class RiskServiceUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 9126890124234613846L;

	public RiskServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}