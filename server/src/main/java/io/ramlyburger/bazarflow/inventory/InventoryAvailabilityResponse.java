package io.ramlyburger.bazarflow.inventory;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryAvailabilityResponse(
		UUID skuId,
		BigDecimal availableQuantity,
		BigDecimal reservedQuantity,
		int availableLotCount
) {
}
