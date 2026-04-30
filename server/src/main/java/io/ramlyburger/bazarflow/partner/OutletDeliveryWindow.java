package io.ramlyburger.bazarflow.partner;

import java.time.LocalTime;
import java.util.UUID;

public record OutletDeliveryWindow(
		UUID outletId,
		String deliveryZone,
		LocalTime deliveryWindowStart,
		LocalTime deliveryWindowEnd
) {
}
