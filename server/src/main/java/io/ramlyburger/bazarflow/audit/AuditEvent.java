package io.ramlyburger.bazarflow.audit;

import io.ramlyburger.bazarflow.common.AuditTrailEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events", schema = "audit")
class AuditEvent {

	@Id
	private UUID id;

	@Column(name = "source_module", nullable = false, length = 64)
	private String sourceModule;

	@Column(name = "aggregate_type", nullable = false, length = 64)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "event_type", nullable = false, length = 96)
	private String eventType;

	@Column(nullable = false, length = 240)
	private String message;

	@Column(nullable = false, length = 120)
	private String actor;

	@Column(name = "correlation_id", length = 120)
	private String correlationId;

	@Column(nullable = false)
	private String details;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AuditEvent() {
	}

	private AuditEvent(AuditTrailEvent event, String actor, String correlationId, String details) {
		this.id = UUID.randomUUID();
		this.sourceModule = event.sourceModule();
		this.aggregateType = event.aggregateType();
		this.aggregateId = event.aggregateId();
		this.eventType = event.eventType();
		this.message = event.message();
		this.actor = actor;
		this.correlationId = correlationId;
		this.details = details;
		this.occurredAt = event.occurredAt();
		this.createdAt = Instant.now();
	}

	static AuditEvent from(AuditTrailEvent event, String actor, String correlationId, String details) {
		return new AuditEvent(event, actor, correlationId, details);
	}

	UUID id() {
		return id;
	}

	String sourceModule() {
		return sourceModule;
	}

	String aggregateType() {
		return aggregateType;
	}

	UUID aggregateId() {
		return aggregateId;
	}

	String eventType() {
		return eventType;
	}

	String message() {
		return message;
	}

	String actor() {
		return actor;
	}

	String correlationId() {
		return correlationId;
	}

	String details() {
		return details;
	}

	Instant occurredAt() {
		return occurredAt;
	}
}
