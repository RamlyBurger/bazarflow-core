package io.ramlyburger.bazarflow.reporting;

import io.ramlyburger.bazarflow.common.ModuleDescriptor;
import io.ramlyburger.bazarflow.common.PlatformStatus;
import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformStatusController {

	private final Clock clock;

	public PlatformStatusController() {
		this(Clock.systemUTC());
	}

	PlatformStatusController(Clock clock) {
		this.clock = clock;
	}

	@GetMapping("/status")
	public PlatformStatus status() {
		return new PlatformStatus(
				"bazarflow-core",
				"active-development",
				clock.instant(),
				List.of(
						new ModuleDescriptor("partner", "Retailers, outlets, zones, and credit status", "implemented"),
						new ModuleDescriptor("catalog", "Products, SKUs, categories, and storage classes", "implemented"),
						new ModuleDescriptor("inventory", "Lots, stock movements, reservations, and expiry risk", "implemented"),
						new ModuleDescriptor("pricing", "Contract pricing, tiers, campaigns, and surcharges", "implemented"),
						new ModuleDescriptor("ordering", "Order intake, idempotency, and state transitions", "implemented"),
						new ModuleDescriptor("fulfillment", "Pick waves, dispatch jobs, delivery outcomes, and SLA risk", "implemented"),
						new ModuleDescriptor("audit", "Append-only operational audit", "implemented"),
						new ModuleDescriptor("reporting", "Operations dashboard read models", "in-progress")
				)
		);
	}
}
