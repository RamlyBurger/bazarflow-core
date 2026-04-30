package io.ramlyburger.bazarflow.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-Id";
	public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));

		request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
		response.setHeader(HEADER_NAME, correlationId);
		MDC.put("correlationId", correlationId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("correlationId");
		}
	}

	private static String resolveCorrelationId(String headerValue) {
		if (headerValue == null || headerValue.isBlank()) {
			return UUID.randomUUID().toString();
		}

		return headerValue.trim();
	}
}
