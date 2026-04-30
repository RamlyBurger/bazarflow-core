package io.ramlyburger.bazarflow.catalog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String name,
		String category,
		String description,
		Map<String, Object> metadata,
		Instant createdAt,
		Instant updatedAt
) {
}
