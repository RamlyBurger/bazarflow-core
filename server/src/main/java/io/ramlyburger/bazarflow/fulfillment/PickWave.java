package io.ramlyburger.bazarflow.fulfillment;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pick_waves", schema = "fulfillment")
class PickWave {

	@Id
	private UUID id;

	@Column(name = "wave_number", nullable = false, length = 48)
	private String waveNumber;

	@Column(name = "delivery_zone", nullable = false, length = 32)
	private String deliveryZone;

	@Column(name = "delivery_date", nullable = false)
	private LocalDate deliveryDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private PickWaveStatus status;

	@Column(name = "order_count", nullable = false)
	private int orderCount;

	@Column(name = "line_count", nullable = false)
	private int lineCount;

	@Column(name = "planned_at", nullable = false)
	private Instant plannedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "pickWave", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DispatchJob> dispatchJobs = new ArrayList<>();

	protected PickWave() {
	}

	private PickWave(String waveNumber, String deliveryZone, LocalDate deliveryDate) {
		this.id = UUID.randomUUID();
		this.waveNumber = waveNumber;
		this.deliveryZone = deliveryZone;
		this.deliveryDate = deliveryDate;
		this.status = PickWaveStatus.OPEN;
		this.plannedAt = Instant.now();
	}

	static PickWave open(String waveNumber, String deliveryZone, LocalDate deliveryDate) {
		return new PickWave(waveNumber, deliveryZone, deliveryDate);
	}

	void addDispatchJob(
			UUID orderId,
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			LocalDate requestedDeliveryDate,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd,
			int orderLineCount,
			boolean slaAtRisk,
			String slaRiskReason
	) {
		dispatchJobs.add(DispatchJob.planned(
				this,
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
		));
		orderCount = dispatchJobs.size();
		lineCount += orderLineCount;
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

	String waveNumber() {
		return waveNumber;
	}

	String deliveryZone() {
		return deliveryZone;
	}

	LocalDate deliveryDate() {
		return deliveryDate;
	}

	PickWaveStatus status() {
		return status;
	}

	int orderCount() {
		return orderCount;
	}

	int lineCount() {
		return lineCount;
	}

	Instant plannedAt() {
		return plannedAt;
	}

	List<DispatchJob> dispatchJobs() {
		return List.copyOf(dispatchJobs);
	}
}
