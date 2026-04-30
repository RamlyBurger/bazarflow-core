package io.ramlyburger.bazarflow.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "price_books", schema = "pricing")
class PriceBook {

	@Id
	private UUID id;

	@Column(nullable = false, length = 64)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, length = 3)
	private String currency;

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

	protected PriceBook() {
	}

	private PriceBook(String code, String name, String currency, LocalDate validFrom, LocalDate validUntil) {
		this.id = UUID.randomUUID();
		this.code = code;
		this.name = name;
		this.currency = currency;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
		this.active = true;
	}

	static PriceBook create(String code, String name, String currency, LocalDate validFrom, LocalDate validUntil) {
		return new PriceBook(code, name, currency, validFrom, validUntil);
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

	String code() {
		return code;
	}

	String name() {
		return name;
	}

	String currency() {
		return currency;
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
