package io.ramlyburger.bazarflow.catalog;

import java.time.Instant;
import java.util.UUID;

public record SkuResponse(
		UUID id,
		UUID productId,
		String productName,
		String skuCode,
		String name,
		String unitOfMeasure,
		StorageClass storageClass,
		Integer shelfLifeDays,
		String barcode,
		SkuStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
