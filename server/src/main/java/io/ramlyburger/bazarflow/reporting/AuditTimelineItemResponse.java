package io.ramlyburger.bazarflow.reporting;

import java.time.Instant;
import java.util.UUID;

record AuditTimelineItemResponse(
		UUID id,
		String sourceModule,
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String message,
		Instant occurredAt
) {
}
