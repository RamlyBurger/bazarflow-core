package io.ramlyburger.bazarflow.partner;

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
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "outlets", schema = "partner")
class Outlet {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "retailer_id", nullable = false)
	private Retailer retailer;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "delivery_zone", nullable = false, length = 32)
	private String deliveryZone;

	@Column(name = "address_line_1", nullable = false, length = 180)
	private String addressLine1;

	@Column(nullable = false, length = 80)
	private String city;

	@Column(nullable = false, length = 80)
	private String state;

	@Column(name = "postal_code", nullable = false, length = 20)
	private String postalCode;

	@Column(name = "delivery_window_start", nullable = false)
	private LocalTime deliveryWindowStart;

	@Column(name = "delivery_window_end", nullable = false)
	private LocalTime deliveryWindowEnd;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Outlet() {
	}

	private Outlet(
			Retailer retailer,
			String name,
			String deliveryZone,
			String addressLine1,
			String city,
			String state,
			String postalCode,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd
	) {
		this.id = UUID.randomUUID();
		this.retailer = retailer;
		this.name = name;
		this.deliveryZone = deliveryZone;
		this.addressLine1 = addressLine1;
		this.city = city;
		this.state = state;
		this.postalCode = postalCode;
		this.deliveryWindowStart = deliveryWindowStart;
		this.deliveryWindowEnd = deliveryWindowEnd;
		this.active = true;
	}

	static Outlet create(
			Retailer retailer,
			String name,
			String deliveryZone,
			String addressLine1,
			String city,
			String state,
			String postalCode,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd
	) {
		return new Outlet(
				retailer,
				name,
				deliveryZone,
				addressLine1,
				city,
				state,
				postalCode,
				deliveryWindowStart,
				deliveryWindowEnd
		);
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

	String deliveryZone() {
		return deliveryZone;
	}

	String addressLine1() {
		return addressLine1;
	}

	String city() {
		return city;
	}

	String state() {
		return state;
	}

	String postalCode() {
		return postalCode;
	}

	LocalTime deliveryWindowStart() {
		return deliveryWindowStart;
	}

	LocalTime deliveryWindowEnd() {
		return deliveryWindowEnd;
	}

	boolean active() {
		return active;
	}
}
