package io.ramlyburger.bazarflow.partner;

import java.time.Instant;
import java.util.UUID;

public record RetailerSummaryResponse(
		UUID id,
		String legalName,
		String tradingName,
		String registrationNumber,
		CreditStatus creditStatus,
		Instant createdAt,
		Instant updatedAt
) {
}
