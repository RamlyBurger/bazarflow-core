package io.ramlyburger.bazarflow.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ramlyburger.bazarflow.common.AuditTrailEvent;
import io.ramlyburger.bazarflow.common.BusinessException;
import io.ramlyburger.bazarflow.common.RequestContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditEventService {

	private static final int MAX_LIMIT = 500;
	private static final TypeReference<Map<String, String>> DETAILS_TYPE = new TypeReference<>() {
	};

	private final AuditEventRepository auditEventRepository;
	private final RequestContext requestContext;
	private final ObjectMapper objectMapper;

	AuditEventService(
			AuditEventRepository auditEventRepository,
			RequestContext requestContext,
			ObjectMapper objectMapper
	) {
		this.auditEventRepository = auditEventRepository;
		this.requestContext = requestContext;
		this.objectMapper = objectMapper;
	}

	@EventListener
	@Transactional
	public void record(AuditTrailEvent event) {
		auditEventRepository.save(AuditEvent.from(
				event,
				requestContext.actor(),
				requestContext.correlationId(),
				serializeDetails(event.details())
		));
	}

	@Transactional(readOnly = true)
	List<AuditEventResponse> listEvents(String aggregateType, UUID aggregateId, int limit) {
		PageRequest pageRequest = PageRequest.of(0, normalizeLimit(limit));
		String normalizedAggregateType = normalizeNullableCode(aggregateType);

		if (normalizedAggregateType != null && aggregateId != null) {
			return auditEventRepository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAscIdAsc(
							normalizedAggregateType,
							aggregateId,
							pageRequest
					)
					.stream()
					.map(this::toResponse)
					.toList();
		}

		if (normalizedAggregateType != null) {
			return auditEventRepository.findByAggregateTypeOrderByOccurredAtDescIdDesc(normalizedAggregateType, pageRequest)
					.stream()
					.map(this::toResponse)
					.toList();
		}

		if (aggregateId != null) {
			return auditEventRepository.findByAggregateIdOrderByOccurredAtDescIdDesc(aggregateId, pageRequest)
					.stream()
					.map(this::toResponse)
					.toList();
		}

		return auditEventRepository.findByOrderByOccurredAtDescIdDesc(pageRequest)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private AuditEventResponse toResponse(AuditEvent event) {
		return new AuditEventResponse(
				event.id(),
				event.sourceModule(),
				event.aggregateType(),
				event.aggregateId(),
				event.eventType(),
				event.message(),
				event.actor(),
				event.correlationId(),
				deserializeDetails(event.details()),
				event.occurredAt()
		);
	}

	private String serializeDetails(Map<String, String> details) {
		try {
			return objectMapper.writeValueAsString(details == null ? Map.of() : details);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Audit event details could not be serialized", exception);
		}
	}

	private Map<String, String> deserializeDetails(String details) {
		try {
			return objectMapper.readValue(details, DETAILS_TYPE);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Audit event details could not be deserialized", exception);
		}
	}

	private static int normalizeLimit(int limit) {
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new BusinessException(
					"INVALID_AUDIT_LIMIT",
					HttpStatus.BAD_REQUEST,
					"Audit event limit must be between 1 and 500"
			);
		}
		return limit;
	}

	private static String normalizeNullableCode(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}
}
