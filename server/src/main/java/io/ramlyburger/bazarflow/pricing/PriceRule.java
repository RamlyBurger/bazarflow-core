package io.ramlyburger.bazarflow.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "price_rules", schema = "pricing")
class PriceRule {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "price_book_id", nullable = false)
	private PriceBook priceBook;

	@Column(name = "rule_code", nullable = false, length = 64)
	private String ruleCode;

	@Column(length = 240)
	private String description;

	@Column(name = "sku_id", nullable = false)
	private UUID skuId;

	@Column(name = "retailer_id")
	private UUID retailerId;

	@Column(name = "delivery_zone", length = 32)
	private String deliveryZone;

	@Column(name = "min_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal minQuantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int priority;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "valid_from", nullable = false)
	private LocalDate validFrom;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected PriceRule() {
	}

	private PriceRule(
			PriceBook priceBook,
			String ruleCode,
			String description,
			UUID skuId,
			UUID retailerId,
			String deliveryZone,
			BigDecimal minQuantity,
			BigDecimal unitPrice,
			int priority,
			LocalDate validFrom,
			LocalDate validUntil
	) {
		this.id = UUID.randomUUID();
		this.priceBook = priceBook;
		this.ruleCode = ruleCode;
		this.description = description;
		this.skuId = skuId;
		this.retailerId = retailerId;
		this.deliveryZone = deliveryZone;
		this.minQuantity = minQuantity;
		this.unitPrice = unitPrice;
		this.priority = priority;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
		this.active = true;
	}

	static PriceRule create(
			PriceBook priceBook,
			String ruleCode,
			String description,
			UUID skuId,
			UUID retailerId,
			String deliveryZone,
			BigDecimal minQuantity,
			BigDecimal unitPrice,
			int priority,
			LocalDate validFrom,
			LocalDate validUntil
	) {
		return new PriceRule(
				priceBook,
				ruleCode,
				description,
				skuId,
				retailerId,
				deliveryZone,
				minQuantity,
				unitPrice,
				priority,
				validFrom,
				validUntil
		);
	}

	int specificity() {
		int specificity = 0;
		if (retailerId != null) {
			specificity += 2;
		}
		if (deliveryZone != null) {
			specificity += 1;
		}
		if (minQuantity.compareTo(BigDecimal.ZERO) > 0) {
			specificity += 1;
		}
		return specificity;
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

	PriceBook priceBook() {
		return priceBook;
	}

	String ruleCode() {
		return ruleCode;
	}

	String description() {
		return description;
	}

	UUID skuId() {
		return skuId;
	}

	UUID retailerId() {
		return retailerId;
	}

	String deliveryZone() {
		return deliveryZone;
	}

	BigDecimal minQuantity() {
		return minQuantity;
	}

	BigDecimal unitPrice() {
		return unitPrice;
	}

	int priority() {
		return priority;
	}

	boolean active() {
		return active;
	}

	LocalDate validFrom() {
		return validFrom;
	}

	LocalDate validUntil() {
		return validUntil;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}
}
