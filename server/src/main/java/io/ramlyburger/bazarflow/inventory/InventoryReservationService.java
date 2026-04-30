package io.ramlyburger.bazarflow.inventory;

import io.ramlyburger.bazarflow.common.BusinessException;
import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

	private final InventoryLotRepository inventoryLotRepository;
	private final InventoryReservationRepository inventoryReservationRepository;
	private final StockMovementRepository stockMovementRepository;

	InventoryReservationService(
			InventoryLotRepository inventoryLotRepository,
			InventoryReservationRepository inventoryReservationRepository,
			StockMovementRepository stockMovementRepository
	) {
		this.inventoryLotRepository = inventoryLotRepository;
		this.inventoryReservationRepository = inventoryReservationRepository;
		this.stockMovementRepository = stockMovementRepository;
	}

	@Transactional
	public InventoryReservationResponse reserve(InventoryReservationCommand command) {
		validateCommand(command);

		if (inventoryReservationRepository.existsByOrderId(command.orderId())) {
			throw new ConflictException("ORDER_ALREADY_RESERVED", "Order already has an inventory reservation");
		}

		InventoryReservation reservation = InventoryReservation.active(command.orderId(), command.requiredByDate());
		for (InventoryReservationItemCommand item : command.items()) {
			reserveItem(reservation, command.orderId(), command.requiredByDate(), item);
		}

		return toResponse(inventoryReservationRepository.save(reservation));
	}

	@Transactional(readOnly = true)
	public List<InventoryReservationResponse> listReservations(UUID orderId) {
		if (orderId != null) {
			return inventoryReservationRepository.findWithLinesByOrderId(orderId)
					.stream()
					.map(InventoryReservationService::toResponse)
					.toList();
		}

		return inventoryReservationRepository.findByOrderByReservedAtDesc()
				.stream()
				.map(InventoryReservationService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public InventoryReservationResponse getReservation(UUID reservationId) {
		return inventoryReservationRepository.findWithLinesById(reservationId)
				.map(InventoryReservationService::toResponse)
				.orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Inventory reservation was not found"));
	}

	private void reserveItem(
			InventoryReservation reservation,
			UUID orderId,
			LocalDate requiredByDate,
			InventoryReservationItemCommand item
	) {
		BigDecimal remaining = item.quantity();
		List<InventoryLot> lots = inventoryLotRepository.findAvailableLotsForReservation(
				item.skuId(),
				LotStatus.AVAILABLE,
				requiredByDate
		);

		for (InventoryLot lot : lots) {
			if (remaining.compareTo(BigDecimal.ZERO) == 0) {
				break;
			}

			BigDecimal reservedQuantity = lot.availableQuantity().min(remaining);
			lot.reserve(reservedQuantity);
			reservation.addLine(lot, reservedQuantity);
			stockMovementRepository.save(StockMovement.reserve(lot, reservedQuantity, orderId));
			remaining = remaining.subtract(reservedQuantity);
		}

		if (remaining.compareTo(BigDecimal.ZERO) > 0) {
			throw new ConflictException("INSUFFICIENT_STOCK", "Not enough available stock for requested SKU");
		}
	}

	private static void validateCommand(InventoryReservationCommand command) {
		if (command == null || command.orderId() == null || command.requiredByDate() == null) {
			throw new BusinessException("INVALID_RESERVATION_REQUEST", HttpStatus.BAD_REQUEST, "Reservation request is invalid");
		}

		if (command.items() == null || command.items().isEmpty()) {
			throw new BusinessException("INVALID_RESERVATION_REQUEST", HttpStatus.BAD_REQUEST, "Reservation items are required");
		}

		Set<UUID> skuIds = new HashSet<>();
		for (InventoryReservationItemCommand item : command.items()) {
			if (item.skuId() == null || item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
				throw new BusinessException(
						"INVALID_RESERVATION_REQUEST",
						HttpStatus.BAD_REQUEST,
						"Reservation item is invalid"
				);
			}
			if (!skuIds.add(item.skuId())) {
				throw new ConflictException("DUPLICATE_RESERVATION_SKU", "Reservation items must not contain duplicate SKUs");
			}
		}
	}

	private static InventoryReservationResponse toResponse(InventoryReservation reservation) {
		return new InventoryReservationResponse(
				reservation.id(),
				reservation.orderId(),
				reservation.requiredByDate(),
				reservation.status(),
				reservation.reservedAt(),
				reservation.expiresAt(),
				reservation.lines()
						.stream()
						.sorted(Comparator.comparing(ReservationLine::expiryDate).thenComparing(ReservationLine::lotCode))
						.map(InventoryReservationService::toLine)
						.toList()
		);
	}

	private static InventoryReservationLineResponse toLine(ReservationLine line) {
		return new InventoryReservationLineResponse(
				line.id(),
				line.lotId(),
				line.skuId(),
				line.lotCode(),
				line.warehouseCode(),
				line.quantity(),
				line.expiryDate()
		);
	}
}
