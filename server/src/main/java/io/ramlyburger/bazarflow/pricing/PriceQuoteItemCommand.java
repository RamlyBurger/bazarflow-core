package io.ramlyburger.bazarflow.pricing;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceQuoteItemCommand(
		UUID skuId,
		BigDecimal quantity
) {
}
