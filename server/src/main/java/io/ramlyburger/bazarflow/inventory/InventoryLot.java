package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_lots", schema = "inventory")
class InventoryLot {

	@Id
	private UUID id;

	@Column(name = "sku_id", nullable = false)
	private UUID skuId;

	@Column(name = "lot_code", nullable = false, length = 64)
	private String lotCode;

	@Column(name = "warehouse_code", nullable = false, length = 32)
	private String warehouseCode;

	@Column(name = "received_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal receivedQuantity;

	@Column(name = "available_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal availableQuantity;

	@Column(name = "reserved_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal reservedQuantity;

	@Column(name = "dispatched_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal dispatchedQuantity;

	@Column(name = "expiry_date", nullable = false)
	private LocalDate expiryDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private LotStatus status;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected InventoryLot() {
	}

	private InventoryLot(
			UUID skuId,
			String lotCode,
			String warehouseCode,
			BigDecimal receivedQuantity,
			LocalDate expiryDate
	) {
		this.id = UUID.randomUUID();
		this.skuId = skuId;
		this.lotCode = lotCode;
		this.warehouseCode = warehouseCode;
		this.receivedQuantity = receivedQuantity;
		this.availableQuantity = receivedQuantity;
		this.reservedQuantity = BigDecimal.ZERO;
		this.dispatchedQuantity = BigDecimal.ZERO;
		this.expiryDate = expiryDate;
		this.status = LotStatus.AVAILABLE;
	}

	static InventoryLot receive(
			UUID skuId,
			String lotCode,
			String warehouseCode,
			BigDecimal receivedQuantity,
			LocalDate expiryDate
	) {
		return new InventoryLot(skuId, lotCode, warehouseCode, receivedQuantity, expiryDate);
	}

	void reserve(BigDecimal quantity) {
		if (status != LotStatus.AVAILABLE || availableQuantity.compareTo(quantity) < 0) {
			throw new IllegalStateException("Inventory lot does not have enough available quantity");
		}

		this.availableQuantity = this.availableQuantity.subtract(quantity);
		this.reservedQuantity = this.reservedQuantity.add(quantity);
	}

	void consumeReserved(BigDecimal quantity) {
		if (quantity.compareTo(BigDecimal.ZERO) <= 0 || reservedQuantity.compareTo(quantity) < 0) {
			throw new IllegalStateException("Inventory lot does not have enough reserved quantity");
		}

		this.reservedQuantity = this.reservedQuantity.subtract(quantity);
		this.dispatchedQuantity = this.dispatchedQuantity.add(quantity);
	}

	@PrePersist
	void markCreated() {
		Instant now = Instant.now();
		this.receivedAt = now;
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void markUpdated() {
		this.updatedAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	UUID skuId() {
		return skuId;
	}

	String lotCode() {
		return lotCode;
	}

	String warehouseCode() {
		return warehouseCode;
	}

	BigDecimal receivedQuantity() {
		return receivedQuantity;
	}

	BigDecimal availableQuantity() {
		return availableQuantity;
	}

	BigDecimal reservedQuantity() {
		return reservedQuantity;
	}

	BigDecimal dispatchedQuantity() {
		return dispatchedQuantity;
	}

	LocalDate expiryDate() {
		return expiryDate;
	}

	LotStatus status() {
		return status;
	}

	Instant receivedAt() {
		return receivedAt;
	}
}
