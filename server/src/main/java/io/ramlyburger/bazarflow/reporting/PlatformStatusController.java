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
				"phase-0-bootstrap",
				clock.instant(),
				List.of(
						new ModuleDescriptor("partner", "Retailers, outlets, zones, and credit status", "planned"),
						new ModuleDescriptor("catalog", "Products, SKUs, categories, and storage classes", "planned"),
						new ModuleDescriptor("inventory", "Lots, stock movements, reservations, and expiry risk", "planned"),
						new ModuleDescriptor("pricing", "Contract pricing, tiers, campaigns, and surcharges", "planned"),
						new ModuleDescriptor("ordering", "Order intake, idempotency, and state transitions", "planned"),
						new ModuleDescriptor("fulfillment", "Pick waves, dispatch jobs, and SLA risk", "planned"),
						new ModuleDescriptor("audit", "Append-only operational audit and hash-chain checks", "planned"),
						new ModuleDescriptor("reporting", "Operations dashboard read models", "bootstrap")
				)
		);
	}
}
