package io.ramlyburger.bazarflow.reporting;

import java.time.Instant;
import java.util.List;

record ReportingDashboardResponse(
		Instant checkedAt,
		List<DashboardKpiResponse> kpis,
		List<WorkQueueItemResponse> workQueue,
		List<RiskItemResponse> risks,
		List<ThroughputPointResponse> throughput,
		List<DispatchBacklogResponse> dispatchBacklog,
		List<AuditTimelineItemResponse> auditTimeline
) {
}
