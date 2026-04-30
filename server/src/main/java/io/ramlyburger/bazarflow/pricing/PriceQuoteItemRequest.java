package io.ramlyburger.bazarflow.pricing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PriceQuoteItemRequest(
		@NotNull UUID skuId,
		@NotNull @Positive BigDecimal quantity
) {
	PriceQuoteItemCommand toCommand() {
		return new PriceQuoteItemCommand(skuId, quantity);
	}
}
