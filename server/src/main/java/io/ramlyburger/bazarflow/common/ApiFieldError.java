package io.ramlyburger.bazarflow.common;

public record ApiFieldError(
		String field,
		String message
) {
}
