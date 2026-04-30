package io.ramlyburger.bazarflow.catalog;

import jakarta.validation.constraints.NotNull;

public record UpdateSkuStatusRequest(
		@NotNull SkuStatus status
) {
}
