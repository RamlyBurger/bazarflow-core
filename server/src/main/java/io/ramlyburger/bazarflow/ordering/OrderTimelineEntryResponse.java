package io.ramlyburger.bazarflow.ordering;

import java.time.Instant;
import java.util.UUID;

public record OrderTimelineEntryResponse(
		UUID id,
		OrderStatus fromStatus,
		OrderStatus toStatus,
		String reason,
		String changedBy,
		Instant changedAt
) {
}
