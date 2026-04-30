package io.ramlyburger.bazarflow.partner;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RetailerDetailsResponse(
		UUID id,
		String legalName,
		String tradingName,
		String registrationNumber,
		CreditStatus creditStatus,
		Instant createdAt,
		Instant updatedAt,
		List<OutletResponse> outlets
) {
}
