package io.ramlyburger.bazarflow.ordering;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

	private final OrderService orderService;

	OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES')")
	ResponseEntity<OrderResponse> createDraft(@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse response = orderService.createDraft(request);
		return ResponseEntity
				.created(URI.create("/api/orders/" + response.id()))
				.body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'WAREHOUSE', 'AUDITOR')")
	List<OrderSummaryResponse> listOrders() {
		return orderService.listOrders();
	}

	@GetMapping("/{orderId}")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'WAREHOUSE', 'AUDITOR')")
	OrderResponse getOrder(@PathVariable UUID orderId) {
		return orderService.getOrder(orderId);
	}

	@PostMapping("/{orderId}/submit")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES')")
	OrderResponse submit(
			@PathVariable UUID orderId,
			@RequestHeader("Idempotency-Key") String idempotencyKey
	) {
		return orderService.submit(orderId, idempotencyKey);
	}

	@PostMapping("/{orderId}/accept")
	@PreAuthorize("hasRole('OPS_MANAGER')")
	OrderResponse accept(@PathVariable UUID orderId) {
		return orderService.accept(orderId);
	}

	@GetMapping("/{orderId}/timeline")
	@PreAuthorize("hasAnyRole('OPS_MANAGER', 'SALES', 'WAREHOUSE', 'AUDITOR')")
	List<OrderTimelineEntryResponse> getTimeline(@PathVariable UUID orderId) {
		return orderService.getTimeline(orderId);
	}
}
