package io.ramlyburger.bazarflow.ordering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
		@NotNull UUID retailerId,
		@NotNull UUID outletId,
		@NotNull @FutureOrPresent LocalDate requestedDeliveryDate,
		@NotEmpty @Size(max = 100) List<@Valid CreateOrderLineRequest> lines
) {
}
