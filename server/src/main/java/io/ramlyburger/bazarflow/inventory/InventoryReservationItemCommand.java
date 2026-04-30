package io.ramlyburger.bazarflow.inventory;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryReservationItemCommand(
		UUID skuId,
		BigDecimal quantity
) {
}
