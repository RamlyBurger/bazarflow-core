package io.ramlyburger.bazarflow.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePriceBookRequest(
		@NotBlank @Size(max = 64) String code,
		@NotBlank @Size(max = 160) String name,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
		@NotNull LocalDate validFrom,
		LocalDate validUntil
) {
}
