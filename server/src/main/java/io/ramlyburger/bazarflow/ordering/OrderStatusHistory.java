package io.ramlyburger.bazarflow.ordering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history", schema = "ordering")
class OrderStatusHistory {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 32)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 32)
	private OrderStatus toStatus;

	@Column(nullable = false, length = 160)
	private String reason;

	@Column(name = "changed_by", nullable = false, length = 120)
	private String changedBy;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	protected OrderStatusHistory() {
	}

	private OrderStatusHistory(
			UUID orderId,
			OrderStatus fromStatus,
			OrderStatus toStatus,
			String reason,
			String changedBy
	) {
		this.id = UUID.randomUUID();
		this.orderId = orderId;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.reason = reason;
		this.changedBy = changedBy;
		this.changedAt = Instant.now();
	}

	static OrderStatusHistory created(UUID orderId, String reason, String changedBy) {
		return new OrderStatusHistory(orderId, null, OrderStatus.DRAFT, reason, changedBy);
	}

	static OrderStatusHistory transition(
			UUID orderId,
			OrderStatus fromStatus,
			OrderStatus toStatus,
			String reason,
			String changedBy
	) {
		return new OrderStatusHistory(orderId, fromStatus, toStatus, reason, changedBy);
	}

	UUID id() {
		return id;
	}

	OrderStatus fromStatus() {
		return fromStatus;
	}

	OrderStatus toStatus() {
		return toStatus;
	}

	String reason() {
		return reason;
	}

	String changedBy() {
		return changedBy;
	}

	Instant changedAt() {
		return changedAt;
	}
}
