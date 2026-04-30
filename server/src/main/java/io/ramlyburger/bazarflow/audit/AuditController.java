package io.ramlyburger.bazarflow.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuditController {

	private final AuditEventService auditEventService;

	AuditController(AuditEventService auditEventService) {
		this.auditEventService = auditEventService;
	}

	@GetMapping("/api/audit/events")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'AUDITOR')")
	List<AuditEventResponse> listEvents(
			@RequestParam(required = false) String aggregateType,
			@RequestParam(required = false) UUID aggregateId,
			@RequestParam(defaultValue = "100") int limit
	) {
		return auditEventService.listEvents(aggregateType, aggregateId, limit);
	}
}
