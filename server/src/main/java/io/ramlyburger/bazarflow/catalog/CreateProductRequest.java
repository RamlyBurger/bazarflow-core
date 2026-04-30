package io.ramlyburger.bazarflow.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateProductRequest(
		@NotBlank @Size(max = 160) String name,
		@NotBlank @Size(max = 80) String category,
		@Size(max = 500) String description,
		Map<String, Object> metadata
) {
}
