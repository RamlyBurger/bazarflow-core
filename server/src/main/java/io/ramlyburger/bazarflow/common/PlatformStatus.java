package io.ramlyburger.bazarflow.common;

import java.time.Instant;
import java.util.List;

public record PlatformStatus(
		String service,
		String phase,
		Instant checkedAt,
		List<ModuleDescriptor> modules
) {
}
