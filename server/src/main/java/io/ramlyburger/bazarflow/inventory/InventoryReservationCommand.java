package io.ramlyburger.bazarflow.inventory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReservationCommand(
		UUID orderId,
		LocalDate requiredByDate,
		List<InventoryReservationItemCommand> items
) {
}
