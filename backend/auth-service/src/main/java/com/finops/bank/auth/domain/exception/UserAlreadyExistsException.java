package com.finops.bank.auth.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = -2651007657677346789L;

	public UserAlreadyExistsException(String message) {
        super(message);
    }
}