package io.ramlyburger.bazarflow.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestContext {

	private static final String SYSTEM_ACTOR = "system";

	public String actor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			return SYSTEM_ACTOR;
		}
		return authentication.getName();
	}

	public String correlationId() {
		String mdcCorrelationId = MDC.get("correlationId");
		if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
			return mdcCorrelationId;
		}

		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			HttpServletRequest request = attributes.getRequest();
			Object requestCorrelationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
			if (requestCorrelationId instanceof String value && !value.isBlank()) {
				return value;
			}
		}

		return null;
	}
}
