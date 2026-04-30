package io.ramlyburger.bazarflow.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final String errorCode;
	private final HttpStatus status;

	public BusinessException(String errorCode, HttpStatus status, String message) {
		super(message);
		this.errorCode = errorCode;
		this.status = status;
	}

	public String errorCode() {
		return errorCode;
	}

	public HttpStatus status() {
		return status;
	}
}
