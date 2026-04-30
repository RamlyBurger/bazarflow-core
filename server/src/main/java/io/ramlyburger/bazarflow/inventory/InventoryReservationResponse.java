package io.ramlyburger.bazarflow.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(
		UUID id,
		UUID orderId,
		LocalDate requiredByDate,
		ReservationStatus status,
		Instant reservedAt,
		Instant expiresAt,
		List<InventoryReservationLineResponse> lines
) {
}
