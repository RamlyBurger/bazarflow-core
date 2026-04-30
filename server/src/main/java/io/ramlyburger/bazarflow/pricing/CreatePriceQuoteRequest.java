package io.ramlyburger.bazarflow.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreatePriceQuoteRequest(
		@NotNull UUID retailerId,
		@NotBlank @Size(max = 32) String deliveryZone,
		@NotEmpty @Size(max = 100) List<@Valid PriceQuoteItemRequest> items
) {
	PriceQuoteCommand toCommand() {
		return new PriceQuoteCommand(
				retailerId,
				deliveryZone,
				items.stream()
						.map(PriceQuoteItemRequest::toCommand)
						.toList()
		);
	}
}
