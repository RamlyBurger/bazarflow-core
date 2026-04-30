package io.ramlyburger.bazarflow.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products", schema = "catalog")
class Product {

	@Id
	private UUID id;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, length = 80)
	private String category;

	@Column(length = 500)
	private String description;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Product() {
	}

	private Product(String name, String category, String description, Map<String, Object> metadata) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.category = category;
		this.description = description;
		this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}

	static Product create(String name, String category, String description, Map<String, Object> metadata) {
		return new Product(name, category, description, metadata);
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

	String name() {
		return name;
	}

	String category() {
		return category;
	}

	String description() {
		return description;
	}

	Map<String, Object> metadata() {
		return metadata;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}
}
