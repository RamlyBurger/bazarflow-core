package io.ramlyburger.bazarflow.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryLotResponse(
		UUID id,
		UUID skuId,
		String lotCode,
		String warehouseCode,
		BigDecimal receivedQuantity,
		BigDecimal availableQuantity,
		BigDecimal reservedQuantity,
		BigDecimal dispatchedQuantity,
		LocalDate expiryDate,
		LotStatus status,
		Instant receivedAt
) {
}
