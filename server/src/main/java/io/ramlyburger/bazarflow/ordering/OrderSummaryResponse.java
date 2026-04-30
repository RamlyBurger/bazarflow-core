package io.ramlyburger.bazarflow.ordering;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderSummaryResponse(
		UUID id,
		String orderNumber,
		UUID retailerId,
		UUID outletId,
		String deliveryZone,
		LocalDate requestedDeliveryDate,
		OrderStatus status,
		String currency,
		BigDecimal subtotal,
		int lineCount,
		Instant submittedAt,
		Instant createdAt,
		Instant updatedAt
) {
}
