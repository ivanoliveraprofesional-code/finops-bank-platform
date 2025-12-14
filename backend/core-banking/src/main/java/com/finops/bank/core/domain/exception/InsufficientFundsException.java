package com.finops.bank.core.domain.exception;

public class InsufficientFundsException extends RuntimeException {

	private static final long serialVersionUID = 5371263966155659687L;

	public InsufficientFundsException(String message) { super(message); }
}
