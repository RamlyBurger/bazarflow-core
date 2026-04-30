package io.ramlyburger.bazarflow.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record AuditEventResponse(
		UUID id,
		String sourceModule,
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String message,
		String actor,
		String correlationId,
		Map<String, String> details,
		Instant occurredAt
) {
}
