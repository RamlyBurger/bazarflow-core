package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reservation_lines", schema = "inventory")
class ReservationLine {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private InventoryReservation reservation;

	@Column(name = "lot_id", nullable = false)
	private UUID lotId;

	@Column(name = "sku_id", nullable = false)
	private UUID skuId;

	@Column(name = "lot_code", nullable = false, length = 64)
	private String lotCode;

	@Column(name = "warehouse_code", nullable = false, length = 32)
	private String warehouseCode;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal quantity;

	@Column(name = "expiry_date", nullable = false)
	private LocalDate expiryDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ReservationLine() {
	}

	private ReservationLine(InventoryReservation reservation, InventoryLot lot, BigDecimal quantity) {
		this.id = UUID.randomUUID();
		this.reservation = reservation;
		this.lotId = lot.id();
		this.skuId = lot.skuId();
		this.lotCode = lot.lotCode();
		this.warehouseCode = lot.warehouseCode();
		this.quantity = quantity;
		this.expiryDate = lot.expiryDate();
	}

	static ReservationLine create(InventoryReservation reservation, InventoryLot lot, BigDecimal quantity) {
		return new ReservationLine(reservation, lot, quantity);
	}

	@PrePersist
	void markCreated() {
		this.createdAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	UUID lotId() {
		return lotId;
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

	BigDecimal quantity() {
		return quantity;
	}

	LocalDate expiryDate() {
		return expiryDate;
	}
}
