package io.ramlyburger.bazarflow.inventory;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InventoryController {

	private final InventoryService inventoryService;

	InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/api/inventory/lots")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE')")
	ResponseEntity<InventoryLotResponse> receiveLot(@Valid @RequestBody CreateInventoryLotRequest request) {
		InventoryLotResponse response = inventoryService.receiveLot(request);
		return ResponseEntity
				.created(URI.create("/api/inventory/lots/" + response.id()))
				.body(response);
	}

	@GetMapping("/api/inventory/lots")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'AUDITOR')")
	List<InventoryLotResponse> listLots() {
		return inventoryService.listLots();
	}

	@GetMapping("/api/inventory/availability")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'AUDITOR')")
	InventoryAvailabilityResponse getAvailability(@RequestParam UUID skuId) {
		return inventoryService.getAvailability(skuId);
	}
}
