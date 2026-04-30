package io.ramlyburger.bazarflow.pricing;

import java.util.List;
import java.util.UUID;

public record PriceQuoteCommand(
		UUID retailerId,
		String deliveryZone,
		List<PriceQuoteItemCommand> items
) {
}
