package io.ramlyburger.bazarflow.ordering;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "ordering")
class CustomerOrder {

	@Id
	private UUID id;

	@Column(name = "order_number", nullable = false, length = 32)
	private String orderNumber;

	@Column(name = "retailer_id", nullable = false)
	private UUID retailerId;

	@Column(name = "outlet_id", nullable = false)
	private UUID outletId;

	@Column(name = "delivery_zone", nullable = false, length = 32)
	private String deliveryZone;

	@Column(name = "requested_delivery_date", nullable = false)
	private LocalDate requestedDeliveryDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OrderStatus status;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderLine> lines = new ArrayList<>();

	protected CustomerOrder() {
	}

	private CustomerOrder(
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			String deliveryZone,
			LocalDate requestedDeliveryDate,
			String currency,
			BigDecimal subtotal
	) {
		this.id = UUID.randomUUID();
		this.orderNumber = orderNumber;
		this.retailerId = retailerId;
		this.outletId = outletId;
		this.deliveryZone = deliveryZone;
		this.requestedDeliveryDate = requestedDeliveryDate;
		this.currency = currency;
		this.subtotal = subtotal;
		this.status = OrderStatus.DRAFT;
	}

	static CustomerOrder draft(
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			String deliveryZone,
			LocalDate requestedDeliveryDate,
			String currency,
			BigDecimal subtotal
	) {
		return new CustomerOrder(
				orderNumber,
				retailerId,
				outletId,
				deliveryZone,
				requestedDeliveryDate,
				currency,
				subtotal
		);
	}

	void addLine(int lineNumber, UUID skuId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
		lines.add(OrderLine.create(this, lineNumber, skuId, quantity, unitPrice, lineTotal));
	}

	void submit() {
		if (status != OrderStatus.DRAFT) {
			throw new IllegalStateException("Only draft orders can be submitted");
		}
		status = OrderStatus.SUBMITTED;
		submittedAt = Instant.now();
	}

	void accept() {
		if (status != OrderStatus.SUBMITTED) {
			throw new IllegalStateException("Only submitted orders can be accepted");
		}
		status = OrderStatus.ACCEPTED;
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

	String orderNumber() {
		return orderNumber;
	}

	UUID retailerId() {
		return retailerId;
	}

	UUID outletId() {
		return outletId;
	}

	String deliveryZone() {
		return deliveryZone;
	}

	LocalDate requestedDeliveryDate() {
		return requestedDeliveryDate;
	}

	OrderStatus status() {
		return status;
	}

	String currency() {
		return currency;
	}

	BigDecimal subtotal() {
		return subtotal;
	}

	Instant submittedAt() {
		return submittedAt;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}

	List<OrderLine> lines() {
		return List.copyOf(lines);
	}
}
