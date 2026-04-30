package io.ramlyburger.bazarflow.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PriceQuoteResponse(
		UUID retailerId,
		String deliveryZone,
		String currency,
		List<PriceQuoteLineResponse> lines,
		BigDecimal subtotal,
		int appliedRuleCount
) {
}
