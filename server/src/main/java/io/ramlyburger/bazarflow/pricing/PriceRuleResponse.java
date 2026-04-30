package io.ramlyburger.bazarflow.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceRuleResponse(
		UUID id,
		UUID priceBookId,
		String priceBookCode,
		String ruleCode,
		String description,
		UUID skuId,
		UUID retailerId,
		String deliveryZone,
		BigDecimal minQuantity,
		BigDecimal unitPrice,
		String currency,
		int priority,
		boolean active,
		LocalDate validFrom,
		LocalDate validUntil,
		Instant createdAt,
		Instant updatedAt
) {
}
