package io.ramlyburger.bazarflow.ordering;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
		UUID id,
		int lineNumber,
		UUID skuId,
		BigDecimal quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal
) {
}
