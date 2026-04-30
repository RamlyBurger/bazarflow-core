package io.ramlyburger.bazarflow.pricing;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
class PricingController {

	private final PricingService pricingService;

	PricingController(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@PostMapping("/price-books")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	ResponseEntity<PriceBookResponse> createPriceBook(@Valid @RequestBody CreatePriceBookRequest request) {
		PriceBookResponse response = pricingService.createPriceBook(request);
		return ResponseEntity
				.created(URI.create("/api/pricing/price-books/" + response.id()))
				.body(response);
	}

	@GetMapping("/price-books")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'AUDITOR')")
	List<PriceBookResponse> listPriceBooks() {
		return pricingService.listPriceBooks();
	}

	@PostMapping("/rules")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	ResponseEntity<PriceRuleResponse> createRule(@Valid @RequestBody CreatePriceRuleRequest request) {
		PriceRuleResponse response = pricingService.createRule(request);
		return ResponseEntity
				.created(URI.create("/api/pricing/rules/" + response.id()))
				.body(response);
	}

	@GetMapping("/rules")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'AUDITOR')")
	List<PriceRuleResponse> listRules() {
		return pricingService.listRules();
	}

	@PostMapping("/quote")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES')")
	PriceQuoteResponse quote(@Valid @RequestBody CreatePriceQuoteRequest request) {
		return pricingService.quote(request.toCommand());
	}
}
