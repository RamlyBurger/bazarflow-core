package io.ramlyburger.bazarflow.reporting;

import java.time.LocalDate;

record ThroughputPointResponse(
		LocalDate day,
		int submittedOrders,
		int deliveredOrders
) {
}
