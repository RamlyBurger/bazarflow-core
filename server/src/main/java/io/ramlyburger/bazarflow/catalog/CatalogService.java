package io.ramlyburger.bazarflow.catalog;

import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogService {

	private static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
	private static final String SKU_NOT_FOUND = "SKU_NOT_FOUND";

	private final ProductRepository productRepository;
	private final SkuRepository skuRepository;

	CatalogService(ProductRepository productRepository, SkuRepository skuRepository) {
		this.productRepository = productRepository;
		this.skuRepository = skuRepository;
	}

	@Transactional
	ProductResponse createProduct(CreateProductRequest request) {
		String name = normalizeRequired(request.name());

		if (productRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("PRODUCT_NAME_ALREADY_EXISTS", "Product name already exists");
		}

		Product product = Product.create(
				name,
				normalizeRequired(request.category()),
				normalizeOptional(request.description()),
				request.metadata()
		);

		return toProduct(productRepository.save(product));
	}

	@Transactional(readOnly = true)
	List<ProductResponse> listProducts() {
		return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
				.stream()
				.map(CatalogService::toProduct)
				.toList();
	}

	@Transactional
	SkuResponse createSku(CreateSkuRequest request) {
		String skuCode = normalizeCode(request.skuCode());

		if (skuRepository.existsBySkuCode(skuCode)) {
			throw new ConflictException("SKU_CODE_ALREADY_EXISTS", "SKU code already exists");
		}

		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new NotFoundException(PRODUCT_NOT_FOUND, "Product was not found"));

		Sku sku = Sku.create(
				product,
				skuCode,
				normalizeRequired(request.name()),
				normalizeRequired(request.unitOfMeasure()).toUpperCase(Locale.ROOT),
				request.storageClass(),
				request.shelfLifeDays(),
				normalizeOptional(request.barcode())
		);

		return toSku(skuRepository.save(sku));
	}

	@Transactional(readOnly = true)
	List<SkuResponse> listSkus() {
		return skuRepository.findByOrderBySkuCodeAsc()
				.stream()
				.map(CatalogService::toSku)
				.toList();
	}

	@Transactional(readOnly = true)
	SkuResponse getSku(UUID skuId) {
		return toSku(findSku(skuId));
	}

	@Transactional
	SkuResponse updateSkuStatus(UUID skuId, UpdateSkuStatusRequest request) {
		Sku sku = findSku(skuId);
		sku.changeStatus(request.status());
		return toSku(sku);
	}

	private Sku findSku(UUID skuId) {
		return skuRepository.findWithProductById(skuId)
				.orElseThrow(() -> new NotFoundException(SKU_NOT_FOUND, "SKU was not found"));
	}

	private static String normalizeRequired(String value) {
		return value.trim();
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private static String normalizeCode(String value) {
		return normalizeRequired(value).toUpperCase(Locale.ROOT);
	}

	private static ProductResponse toProduct(Product product) {
		return new ProductResponse(
				product.id(),
				product.name(),
				product.category(),
				product.description(),
				product.metadata(),
				product.createdAt(),
				product.updatedAt()
		);
	}

	private static SkuResponse toSku(Sku sku) {
		Product product = sku.product();
		return new SkuResponse(
				sku.id(),
				product.id(),
				product.name(),
				sku.skuCode(),
				sku.name(),
				sku.unitOfMeasure(),
				sku.storageClass(),
				sku.shelfLifeDays(),
				sku.barcode(),
				sku.status(),
				sku.createdAt(),
				sku.updatedAt()
		);
	}
}
