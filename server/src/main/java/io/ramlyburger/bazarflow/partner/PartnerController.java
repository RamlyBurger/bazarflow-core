package io.ramlyburger.bazarflow.partner;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retailers")
class PartnerController {

	private final PartnerService partnerService;

	PartnerController(PartnerService partnerService) {
		this.partnerService = partnerService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES')")
	ResponseEntity<RetailerDetailsResponse> createRetailer(@Valid @RequestBody CreateRetailerRequest request) {
		RetailerDetailsResponse response = partnerService.createRetailer(request);
		return ResponseEntity
				.created(URI.create("/api/retailers/" + response.id()))
				.body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'AUDITOR')")
	List<RetailerSummaryResponse> listRetailers() {
		return partnerService.listRetailers();
	}

	@GetMapping("/{retailerId}")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'AUDITOR')")
	RetailerDetailsResponse getRetailer(@PathVariable UUID retailerId) {
		return partnerService.getRetailer(retailerId);
	}

	@PostMapping("/{retailerId}/outlets")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES')")
	ResponseEntity<OutletResponse> createOutlet(
			@PathVariable UUID retailerId,
			@Valid @RequestBody CreateOutletRequest request
	) {
		OutletResponse response = partnerService.createOutlet(retailerId, request);
		return ResponseEntity
				.created(URI.create("/api/retailers/" + retailerId + "/outlets/" + response.id()))
				.body(response);
	}

	@PatchMapping("/{retailerId}/credit-status")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	RetailerDetailsResponse updateCreditStatus(
			@PathVariable UUID retailerId,
			@Valid @RequestBody UpdateCreditStatusRequest request
	) {
		return partnerService.updateCreditStatus(retailerId, request);
	}
}
