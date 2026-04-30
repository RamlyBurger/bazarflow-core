package io.ramlyburger.bazarflow.reporting;

record DispatchBacklogResponse(
		String deliveryZone,
		int openJobs,
		int atRiskJobs
) {
}
