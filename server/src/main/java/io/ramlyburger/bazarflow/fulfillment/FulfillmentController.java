package io.ramlyburger.bazarflow.fulfillment;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fulfillment")
class FulfillmentController {

	private final FulfillmentService fulfillmentService;

	FulfillmentController(FulfillmentService fulfillmentService) {
		this.fulfillmentService = fulfillmentService;
	}

	@PostMapping("/pick-waves")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'DISPATCH')")
	ResponseEntity<PickWaveResponse> createPickWave(@Valid @RequestBody CreatePickWaveRequest request) {
		PickWaveResponse response = fulfillmentService.createPickWave(request);
		return ResponseEntity
				.created(URI.create("/api/fulfillment/pick-waves/" + response.id()))
				.body(response);
	}

	@GetMapping("/pick-waves")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'DISPATCH', 'WAREHOUSE', 'AUDITOR')")
	List<PickWaveResponse> listPickWaves() {
		return fulfillmentService.listPickWaves();
	}

	@GetMapping("/pick-waves/{pickWaveId}")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'DISPATCH', 'WAREHOUSE', 'AUDITOR')")
	PickWaveResponse getPickWave(@PathVariable UUID pickWaveId) {
		return fulfillmentService.getPickWave(pickWaveId);
	}

	@GetMapping("/sla-risk")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'DISPATCH', 'WAREHOUSE', 'AUDITOR')")
	List<DispatchJobResponse> listSlaRiskJobs() {
		return fulfillmentService.listSlaRiskJobs();
	}
}
