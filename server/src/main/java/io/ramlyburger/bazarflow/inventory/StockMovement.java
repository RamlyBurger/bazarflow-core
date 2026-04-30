package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", schema = "inventory")
class StockMovement {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lot_id", nullable = false)
	private InventoryLot lot;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 32)
	private StockMovementType movementType;

	@Column(name = "quantity_delta", nullable = false, precision = 12, scale = 3)
	private BigDecimal quantityDelta;

	@Column(nullable = false, length = 160)
	private String reason;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected StockMovement() {
	}

	private StockMovement(InventoryLot lot, StockMovementType movementType, BigDecimal quantityDelta, String reason) {
		this.id = UUID.randomUUID();
		this.lot = lot;
		this.movementType = movementType;
		this.quantityDelta = quantityDelta;
		this.reason = reason;
		this.occurredAt = Instant.now();
	}

	static StockMovement receive(InventoryLot lot) {
		return new StockMovement(lot, StockMovementType.RECEIVE, lot.receivedQuantity(), "Initial lot receipt");
	}

	static StockMovement reserve(InventoryLot lot, BigDecimal quantity, UUID orderId) {
		return new StockMovement(
				lot,
				StockMovementType.RESERVE,
				quantity.negate(),
				"Reserved for order " + orderId
		);
	}

	static StockMovement dispatch(InventoryLot lot, BigDecimal quantity, UUID orderId) {
		return new StockMovement(
				lot,
				StockMovementType.DISPATCH,
				quantity.negate(),
				"Dispatched for order " + orderId
		);
	}
}
