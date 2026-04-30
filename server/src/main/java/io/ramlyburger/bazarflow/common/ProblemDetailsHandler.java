package io.ramlyburger.bazarflow.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ProblemDetail> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle(exception.errorCode());
		problem.setProperty("errorCode", exception.errorCode());
		addCorrelationId(problem, request);
		return ResponseEntity.status(exception.status()).body(problem);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
			DataIntegrityViolationException exception,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"Request conflicts with existing data or database constraints"
		);
		problem.setTitle("DATA_CONSTRAINT_VIOLATION");
		problem.setProperty("errorCode", "DATA_CONSTRAINT_VIOLATION");
		addCorrelationId(problem, request);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled request failure", exception);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Unexpected server error"
		);
		problem.setTitle("INTERNAL_SERVER_ERROR");
		problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
		addCorrelationId(problem, request);
		return ResponseEntity.internalServerError().body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Request validation failed"
		);
		problem.setTitle("VALIDATION_FAILED");
		problem.setProperty("errorCode", "VALIDATION_FAILED");
		problem.setProperty("errors", fieldErrors(exception));
		addCorrelationId(problem, request);
		return ResponseEntity.badRequest().body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception exception,
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request
	) {
		Object responseBody = body;

		if (responseBody instanceof ProblemDetail problem) {
			addCorrelationId(problem, request);
		} else if (responseBody == null) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusCode, "Request could not be processed");
			problem.setTitle("REQUEST_ERROR");
			problem.setProperty("errorCode", "REQUEST_ERROR");
			addCorrelationId(problem, request);
			responseBody = problem;
		}

		return super.handleExceptionInternal(exception, responseBody, headers, statusCode, request);
	}

	private static List<ApiFieldError> fieldErrors(MethodArgumentNotValidException exception) {
		return exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(ProblemDetailsHandler::toApiFieldError)
				.toList();
	}

	private static ApiFieldError toApiFieldError(FieldError error) {
		return new ApiFieldError(error.getField(), error.getDefaultMessage());
	}

	private static void addCorrelationId(ProblemDetail problem, WebRequest request) {
		if (request instanceof ServletWebRequest servletWebRequest) {
			addCorrelationId(problem, servletWebRequest.getRequest());
		}
	}

	private static void addCorrelationId(ProblemDetail problem, HttpServletRequest request) {
		Object correlationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
		if (correlationId instanceof String value) {
			problem.setProperty("correlationId", value);
		}
	}
}
