package io.ramlyburger.bazarflow.fulfillment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record FailDispatchJobRequest(
		@NotBlank
		@Size(max = 160)
		String reason
) {
}
