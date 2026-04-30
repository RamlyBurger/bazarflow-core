package io.ramlyburger.bazarflow.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryLotRequest(
		@NotNull UUID skuId,
		@NotBlank @Size(max = 64) String lotCode,
		@NotBlank @Size(max = 32) String warehouseCode,
		@NotNull @Positive BigDecimal receivedQuantity,
		@NotNull LocalDate expiryDate
) {
}
