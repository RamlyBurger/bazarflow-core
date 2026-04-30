package io.ramlyburger.bazarflow.partner;

import java.time.LocalTime;
import java.util.UUID;

public record OutletResponse(
		UUID id,
		String name,
		String deliveryZone,
		String addressLine1,
		String city,
		String state,
		String postalCode,
		LocalTime deliveryWindowStart,
		LocalTime deliveryWindowEnd,
		boolean active
) {
}
