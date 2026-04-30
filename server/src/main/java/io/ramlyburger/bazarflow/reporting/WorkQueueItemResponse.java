package io.ramlyburger.bazarflow.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record WorkQueueItemResponse(
		UUID orderId,
		String orderNumber,
		String outletName,
		String deliveryZone,
		LocalDate requestedDeliveryDate,
		String orderStatus,
		String dispatchStatus,
		BigDecimal totalQuantity,
		boolean slaAtRisk,
		String slaRiskReason,
		LocalTime deliveryWindowStart,
		LocalTime deliveryWindowEnd
) {
}
