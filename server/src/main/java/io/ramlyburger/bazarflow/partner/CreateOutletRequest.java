package io.ramlyburger.bazarflow.partner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateOutletRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 32) String deliveryZone,
		@NotBlank @Size(max = 180) String addressLine1,
		@NotBlank @Size(max = 80) String city,
		@NotBlank @Size(max = 80) String state,
		@NotBlank @Size(max = 20) String postalCode,
		@NotNull LocalTime deliveryWindowStart,
		@NotNull LocalTime deliveryWindowEnd
) {
}
