package io.ramlyburger.bazarflow.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "retailers", schema = "partner")
class Retailer {

	@Id
	private UUID id;

	@Column(name = "legal_name", nullable = false, length = 160)
	private String legalName;

	@Column(name = "trading_name", length = 120)
	private String tradingName;

	@Column(name = "registration_number", length = 64)
	private String registrationNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "credit_status", nullable = false, length = 32)
	private CreditStatus creditStatus;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Retailer() {
	}

	private Retailer(String legalName, String tradingName, String registrationNumber) {
		this.id = UUID.randomUUID();
		this.legalName = legalName;
		this.tradingName = tradingName;
		this.registrationNumber = registrationNumber;
		this.creditStatus = CreditStatus.ACTIVE;
	}

	static Retailer create(String legalName, String tradingName, String registrationNumber) {
		return new Retailer(legalName, tradingName, registrationNumber);
	}

	void changeCreditStatus(CreditStatus creditStatus) {
		this.creditStatus = Objects.requireNonNull(creditStatus, "creditStatus must not be null");
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

	String legalName() {
		return legalName;
	}

	String tradingName() {
		return tradingName;
	}

	String registrationNumber() {
		return registrationNumber;
	}

	CreditStatus creditStatus() {
		return creditStatus;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}
}
