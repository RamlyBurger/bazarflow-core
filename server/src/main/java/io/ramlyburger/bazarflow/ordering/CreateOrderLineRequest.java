package io.ramlyburger.bazarflow.ordering;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderLineRequest(
		@NotNull UUID skuId,
		@NotNull @Positive BigDecimal quantity
) {
}
