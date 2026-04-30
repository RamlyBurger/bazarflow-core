package io.ramlyburger.bazarflow.pricing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceBookResponse(
		UUID id,
		String code,
		String name,
		String currency,
		boolean active,
		LocalDate validFrom,
		LocalDate validUntil,
		Instant createdAt,
		Instant updatedAt
) {
}
