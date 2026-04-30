package io.ramlyburger.bazarflow.partner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRetailerRequest(
		@NotBlank @Size(max = 160) String legalName,
		@Size(max = 120) String tradingName,
		@Size(max = 64) String registrationNumber
) {
}
