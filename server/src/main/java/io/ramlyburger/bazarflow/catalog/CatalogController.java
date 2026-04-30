package io.ramlyburger.bazarflow.catalog;

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
class CatalogController {

	private final CatalogService catalogService;

	CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@PostMapping("/api/products")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
		ProductResponse response = catalogService.createProduct(request);
		return ResponseEntity
				.created(URI.create("/api/products/" + response.id()))
				.body(response);
	}

	@GetMapping("/api/products")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'AUDITOR')")
	List<ProductResponse> listProducts() {
		return catalogService.listProducts();
	}

	@PostMapping("/api/skus")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	ResponseEntity<SkuResponse> createSku(@Valid @RequestBody CreateSkuRequest request) {
		SkuResponse response = catalogService.createSku(request);
		return ResponseEntity
				.created(URI.create("/api/skus/" + response.id()))
				.body(response);
	}

	@GetMapping("/api/skus")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'AUDITOR')")
	List<SkuResponse> listSkus() {
		return catalogService.listSkus();
	}

	@GetMapping("/api/skus/{skuId}")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'WAREHOUSE', 'SALES', 'AUDITOR')")
	SkuResponse getSku(@PathVariable UUID skuId) {
		return catalogService.getSku(skuId);
	}

	@PatchMapping("/api/skus/{skuId}/status")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	SkuResponse updateSkuStatus(
			@PathVariable UUID skuId,
			@Valid @RequestBody UpdateSkuStatusRequest request
	) {
		return catalogService.updateSkuStatus(skuId, request);
	}
}
