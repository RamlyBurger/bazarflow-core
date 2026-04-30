package io.ramlyburger.bazarflow.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

	public NotFoundException(String errorCode, String message) {
		super(errorCode, HttpStatus.NOT_FOUND, message);
	}
}
