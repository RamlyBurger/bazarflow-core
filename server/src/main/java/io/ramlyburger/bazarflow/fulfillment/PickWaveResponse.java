package io.ramlyburger.bazarflow.fulfillment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record PickWaveResponse(
		UUID id,
		String waveNumber,
		String deliveryZone,
		LocalDate deliveryDate,
		PickWaveStatus status,
		int orderCount,
		int lineCount,
		Instant plannedAt,
		List<DispatchJobResponse> dispatchJobs
) {
}
