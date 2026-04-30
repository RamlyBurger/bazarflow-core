package io.ramlyburger.bazarflow.ordering;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
		UUID id,
		String orderNumber,
		UUID retailerId,
		UUID outletId,
		String deliveryZone,
		LocalDate requestedDeliveryDate,
		OrderStatus status,
		String currency,
		BigDecimal subtotal,
		Instant submittedAt,
		Instant createdAt,
		Instant updatedAt,
		List<OrderLineResponse> lines
) {
}
