package io.ramlyburger.bazarflow.fulfillment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

record CreatePickWaveRequest(
		@NotNull LocalDate deliveryDate,
		@NotBlank @Size(max = 32) String deliveryZone
) {
}
