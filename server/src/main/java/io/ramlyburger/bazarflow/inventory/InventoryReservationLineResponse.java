package io.ramlyburger.bazarflow.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryReservationLineResponse(
		UUID id,
		UUID lotId,
		UUID skuId,
		String lotCode,
		String warehouseCode,
		BigDecimal quantity,
		LocalDate expiryDate
) {
}
