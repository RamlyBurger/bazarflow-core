package io.ramlyburger.bazarflow.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSkuRequest(
		@NotNull UUID productId,
		@NotBlank @Size(max = 64) String skuCode,
		@NotBlank @Size(max = 160) String name,
		@NotBlank @Size(max = 24) String unitOfMeasure,
		@NotNull StorageClass storageClass,
		@PositiveOrZero Integer shelfLifeDays,
		@Size(max = 64) String barcode
) {
}
