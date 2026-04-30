package io.ramlyburger.bazarflow.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditTrailEvent(
		String sourceModule,
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String message,
		Map<String, String> details,
		Instant occurredAt
) {

	public AuditTrailEvent {
		sourceModule = requireText(sourceModule, "sourceModule");
		aggregateType = requireText(aggregateType, "aggregateType");
		aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		eventType = requireText(eventType, "eventType");
		message = requireText(message, "message");
		details = sanitizeDetails(details);
		occurredAt = occurredAt == null ? Instant.now() : occurredAt;
	}

	public static AuditTrailEvent record(
			String sourceModule,
			String aggregateType,
			UUID aggregateId,
			String eventType,
			String message,
			Map<String, String> details
	) {
		return new AuditTrailEvent(
				sourceModule,
				aggregateType,
				aggregateId,
				eventType,
				message,
				details,
				Instant.now()
		);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static Map<String, String> sanitizeDetails(Map<String, String> details) {
		if (details == null || details.isEmpty()) {
			return Map.of();
		}

		Map<String, String> sanitized = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : details.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key != null && !key.isBlank() && value != null) {
				sanitized.put(key.trim(), value);
			}
		}
		return Map.copyOf(sanitized);
	}
}
