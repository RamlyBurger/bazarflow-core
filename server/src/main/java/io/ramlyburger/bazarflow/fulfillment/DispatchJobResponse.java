package io.ramlyburger.bazarflow.fulfillment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record DispatchJobResponse(
		UUID id,
		UUID orderId,
		String orderNumber,
		UUID retailerId,
		UUID outletId,
		String deliveryZone,
		LocalDate requestedDeliveryDate,
		LocalTime deliveryWindowStart,
		LocalTime deliveryWindowEnd,
		DispatchJobStatus status,
		boolean slaAtRisk,
		String slaRiskReason,
		Instant plannedAt
) {
}
