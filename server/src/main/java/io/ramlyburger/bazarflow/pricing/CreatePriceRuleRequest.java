package io.ramlyburger.bazarflow.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePriceRuleRequest(
		@NotNull UUID priceBookId,
		@NotBlank @Size(max = 64) String ruleCode,
		@Size(max = 240) String description,
		@NotNull UUID skuId,
		UUID retailerId,
		@Size(max = 32) String deliveryZone,
		@NotNull @PositiveOrZero BigDecimal minQuantity,
		@NotNull @Positive BigDecimal unitPrice,
		int priority,
		@NotNull LocalDate validFrom,
		LocalDate validUntil
) {
}
