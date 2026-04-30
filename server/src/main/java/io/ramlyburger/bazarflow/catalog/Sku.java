package io.ramlyburger.bazarflow.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "skus", schema = "catalog")
class Sku {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "sku_code", nullable = false, length = 64)
	private String skuCode;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "unit_of_measure", nullable = false, length = 24)
	private String unitOfMeasure;

	@Enumerated(EnumType.STRING)
	@Column(name = "storage_class", nullable = false, length = 24)
	private StorageClass storageClass;

	@Column(name = "shelf_life_days")
	private Integer shelfLifeDays;

	@Column(length = 64)
	private String barcode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private SkuStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Sku() {
	}

	private Sku(
			Product product,
			String skuCode,
			String name,
			String unitOfMeasure,
			StorageClass storageClass,
			Integer shelfLifeDays,
			String barcode
	) {
		this.id = UUID.randomUUID();
		this.product = product;
		this.skuCode = skuCode;
		this.name = name;
		this.unitOfMeasure = unitOfMeasure;
		this.storageClass = storageClass;
		this.shelfLifeDays = shelfLifeDays;
		this.barcode = barcode;
		this.status = SkuStatus.ACTIVE;
	}

	static Sku create(
			Product product,
			String skuCode,
			String name,
			String unitOfMeasure,
			StorageClass storageClass,
			Integer shelfLifeDays,
			String barcode
	) {
		return new Sku(product, skuCode, name, unitOfMeasure, storageClass, shelfLifeDays, barcode);
	}

	void changeStatus(SkuStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
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

	Product product() {
		return product;
	}

	String skuCode() {
		return skuCode;
	}

	String name() {
		return name;
	}

	String unitOfMeasure() {
		return unitOfMeasure;
	}

	StorageClass storageClass() {
		return storageClass;
	}

	Integer shelfLifeDays() {
		return shelfLifeDays;
	}

	String barcode() {
		return barcode;
	}

	SkuStatus status() {
		return status;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}
}
