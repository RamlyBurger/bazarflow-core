package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations", schema = "inventory")
class InventoryReservation {

	private static final Duration DEFAULT_EXPIRY = Duration.ofHours(4);

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "required_by_date", nullable = false)
	private LocalDate requiredByDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private ReservationStatus status;

	@Column(name = "reserved_at", nullable = false, updatable = false)
	private Instant reservedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReservationLine> lines = new ArrayList<>();

	protected InventoryReservation() {
	}

	private InventoryReservation(UUID orderId, LocalDate requiredByDate) {
		Instant now = Instant.now();
		this.id = UUID.randomUUID();
		this.orderId = orderId;
		this.requiredByDate = requiredByDate;
		this.status = ReservationStatus.ACTIVE;
		this.reservedAt = now;
		this.expiresAt = now.plus(DEFAULT_EXPIRY);
	}

	static InventoryReservation active(UUID orderId, LocalDate requiredByDate) {
		return new InventoryReservation(orderId, requiredByDate);
	}

	void addLine(InventoryLot lot, java.math.BigDecimal quantity) {
		lines.add(ReservationLine.create(this, lot, quantity));
	}

	@PrePersist
	void markCreated() {
		Instant now = Instant.now();
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

	UUID orderId() {
		return orderId;
	}

	LocalDate requiredByDate() {
		return requiredByDate;
	}

	ReservationStatus status() {
		return status;
	}

	Instant reservedAt() {
		return reservedAt;
	}

	Instant expiresAt() {
		return expiresAt;
	}

	List<ReservationLine> lines() {
		return List.copyOf(lines);
	}
}
