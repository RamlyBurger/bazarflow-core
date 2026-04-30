package io.ramlyburger.bazarflow.ordering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records", schema = "ordering")
class IdempotencyRecord {

	@Id
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, length = 120)
	private String idempotencyKey;

	@Column(name = "command_type", nullable = false, length = 64)
	private String commandType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@Column(name = "response_order_id", nullable = false)
	private UUID responseOrderId;

	@Column(name = "status_code", nullable = false)
	private int statusCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected IdempotencyRecord() {
	}

	private IdempotencyRecord(String idempotencyKey, String commandType, UUID targetId, UUID responseOrderId) {
		this.id = UUID.randomUUID();
		this.idempotencyKey = idempotencyKey;
		this.commandType = commandType;
		this.targetId = targetId;
		this.responseOrderId = responseOrderId;
		this.statusCode = 200;
	}

	static IdempotencyRecord success(String idempotencyKey, String commandType, UUID targetId, UUID responseOrderId) {
		return new IdempotencyRecord(idempotencyKey, commandType, targetId, responseOrderId);
	}

	@PrePersist
	void markCreated() {
		this.createdAt = Instant.now();
	}

	UUID responseOrderId() {
		return responseOrderId;
	}
}
