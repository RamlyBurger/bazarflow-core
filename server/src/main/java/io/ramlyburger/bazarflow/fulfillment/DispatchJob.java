package io.ramlyburger.bazarflow.fulfillment;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "dispatch_jobs", schema = "fulfillment")
class DispatchJob {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pick_wave_id", nullable = false)
	private PickWave pickWave;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

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

	@Column(name = "delivery_window_start", nullable = false)
	private LocalTime deliveryWindowStart;

	@Column(name = "delivery_window_end", nullable = false)
	private LocalTime deliveryWindowEnd;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private DispatchJobStatus status;

	@Column(name = "sla_at_risk", nullable = false)
	private boolean slaAtRisk;

	@Column(name = "sla_risk_reason", length = 120)
	private String slaRiskReason;

	@Column(name = "planned_at", nullable = false)
	private Instant plannedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected DispatchJob() {
	}

	private DispatchJob(
			PickWave pickWave,
			UUID orderId,
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			String deliveryZone,
			LocalDate requestedDeliveryDate,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd,
			boolean slaAtRisk,
			String slaRiskReason
	) {
		this.id = UUID.randomUUID();
		this.pickWave = pickWave;
		this.orderId = orderId;
		this.orderNumber = orderNumber;
		this.retailerId = retailerId;
		this.outletId = outletId;
		this.deliveryZone = deliveryZone;
		this.requestedDeliveryDate = requestedDeliveryDate;
		this.deliveryWindowStart = deliveryWindowStart;
		this.deliveryWindowEnd = deliveryWindowEnd;
		this.status = DispatchJobStatus.PLANNED;
		this.slaAtRisk = slaAtRisk;
		this.slaRiskReason = slaRiskReason;
		this.plannedAt = Instant.now();
	}

	static DispatchJob planned(
			PickWave pickWave,
			UUID orderId,
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			String deliveryZone,
			LocalDate requestedDeliveryDate,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd,
			boolean slaAtRisk,
			String slaRiskReason
	) {
		return new DispatchJob(
				pickWave,
				orderId,
				orderNumber,
				retailerId,
				outletId,
				deliveryZone,
				requestedDeliveryDate,
				deliveryWindowStart,
				deliveryWindowEnd,
				slaAtRisk,
				slaRiskReason
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

	UUID orderId() {
		return orderId;
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

	LocalTime deliveryWindowStart() {
		return deliveryWindowStart;
	}

	LocalTime deliveryWindowEnd() {
		return deliveryWindowEnd;
	}

	DispatchJobStatus status() {
		return status;
	}

	boolean slaAtRisk() {
		return slaAtRisk;
	}

	String slaRiskReason() {
		return slaRiskReason;
	}

	Instant plannedAt() {
		return plannedAt;
	}
}
