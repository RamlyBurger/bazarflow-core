package io.ramlyburger.bazarflow.reporting;

record DashboardKpiResponse(
		String label,
		String value,
		String detail,
		String tone
) {
}
