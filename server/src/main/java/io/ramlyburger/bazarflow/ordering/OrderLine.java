package io.ramlyburger.bazarflow.ordering;

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
import java.util.UUID;

@Entity
@Table(name = "order_lines", schema = "ordering")
class OrderLine {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(name = "sku_id", nullable = false)
	private UUID skuId;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OrderLine() {
	}

	private OrderLine(
			CustomerOrder order,
			int lineNumber,
			UUID skuId,
			BigDecimal quantity,
			BigDecimal unitPrice,
			BigDecimal lineTotal
	) {
		this.id = UUID.randomUUID();
		this.order = order;
		this.lineNumber = lineNumber;
		this.skuId = skuId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.lineTotal = lineTotal;
	}

	static OrderLine create(
			CustomerOrder order,
			int lineNumber,
			UUID skuId,
			BigDecimal quantity,
			BigDecimal unitPrice,
			BigDecimal lineTotal
	) {
		return new OrderLine(order, lineNumber, skuId, quantity, unitPrice, lineTotal);
	}

	@PrePersist
	void markCreated() {
		this.createdAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	int lineNumber() {
		return lineNumber;
	}

	UUID skuId() {
		return skuId;
	}

	BigDecimal quantity() {
		return quantity;
	}

	BigDecimal unitPrice() {
		return unitPrice;
	}

	BigDecimal lineTotal() {
		return lineTotal;
	}
}
