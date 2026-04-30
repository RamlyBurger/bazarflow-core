package io.ramlyburger.bazarflow.inventory;

import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryService {

	private final InventoryLotRepository inventoryLotRepository;
	private final StockMovementRepository stockMovementRepository;
	private final JdbcTemplate jdbcTemplate;

	InventoryService(
			InventoryLotRepository inventoryLotRepository,
			StockMovementRepository stockMovementRepository,
			JdbcTemplate jdbcTemplate
	) {
		this.inventoryLotRepository = inventoryLotRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	InventoryLotResponse receiveLot(CreateInventoryLotRequest request) {
		validateSkuExists(request.skuId());
		validateExpiryDate(request.expiryDate());

		String lotCode = normalizeCode(request.lotCode());
		String warehouseCode = normalizeCode(request.warehouseCode());

		if (inventoryLotRepository.existsBySkuIdAndLotCode(request.skuId(), lotCode)) {
			throw new ConflictException("INVENTORY_LOT_ALREADY_EXISTS", "Inventory lot already exists for SKU");
		}

		InventoryLot lot = InventoryLot.receive(
				request.skuId(),
				lotCode,
				warehouseCode,
				request.receivedQuantity(),
				request.expiryDate()
		);

		InventoryLot savedLot = inventoryLotRepository.save(lot);
		stockMovementRepository.save(StockMovement.receive(savedLot));
		return toLot(savedLot);
	}

	@Transactional(readOnly = true)
	List<InventoryLotResponse> listLots() {
		return inventoryLotRepository.findByOrderByExpiryDateAscLotCodeAsc()
				.stream()
				.map(InventoryService::toLot)
				.toList();
	}

	@Transactional(readOnly = true)
	InventoryAvailabilityResponse getAvailability(UUID skuId) {
		validateSkuExists(skuId);

		List<InventoryLot> lots = inventoryLotRepository.findBySkuIdOrderByExpiryDateAscReceivedAtAsc(skuId);
		List<InventoryLot> availableLots = lots.stream()
				.filter(InventoryService::isAvailableForReservation)
				.toList();

		BigDecimal availableQuantity = availableLots.stream()
				.map(InventoryLot::availableQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal reservedQuantity = lots.stream()
				.map(InventoryLot::reservedQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new InventoryAvailabilityResponse(skuId, availableQuantity, reservedQuantity, availableLots.size());
	}

	private void validateSkuExists(UUID skuId) {
		Boolean exists = jdbcTemplate.queryForObject(
				"select exists(select 1 from catalog.skus where id = ?)",
				Boolean.class,
				skuId
		);

		if (!Boolean.TRUE.equals(exists)) {
			throw new NotFoundException("SKU_NOT_FOUND", "SKU was not found");
		}
	}

	private static void validateExpiryDate(LocalDate expiryDate) {
		if (expiryDate.isBefore(LocalDate.now(ZoneOffset.UTC))) {
			throw new ConflictException("INVALID_EXPIRY_DATE", "Expiry date cannot be in the past");
		}
	}

	private static boolean isAvailableForReservation(InventoryLot lot) {
		return lot.status() == LotStatus.AVAILABLE && lot.availableQuantity().compareTo(BigDecimal.ZERO) > 0;
	}

	private static String normalizeCode(String value) {
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private static InventoryLotResponse toLot(InventoryLot lot) {
		return new InventoryLotResponse(
				lot.id(),
				lot.skuId(),
				lot.lotCode(),
				lot.warehouseCode(),
				lot.receivedQuantity(),
				lot.availableQuantity(),
				lot.reservedQuantity(),
				lot.dispatchedQuantity(),
				lot.expiryDate(),
				lot.status(),
				lot.receivedAt()
		);
	}
}
