package io.ramlyburger.bazarflow.reporting;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporting")
class ReportingController {

	private final ReportingService reportingService;

	ReportingController(ReportingService reportingService) {
		this.reportingService = reportingService;
	}

	@GetMapping("/dashboard")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'DISPATCH', 'AUDITOR')")
	ReportingDashboardResponse dashboard() {
		return reportingService.dashboard();
	}
}
