package io.ramlyburger.bazarflow.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PriceQuoteLineResponse(
		UUID skuId,
		BigDecimal quantity,
		BigDecimal unitPrice,
		String currency,
		BigDecimal lineTotal,
		List<AppliedPriceRuleResponse> appliedRules
) {
}
