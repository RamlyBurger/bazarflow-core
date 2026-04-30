package io.ramlyburger.bazarflow.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

	List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtAscIdAsc(
			String aggregateType,
			UUID aggregateId,
			Pageable pageable
	);

	List<AuditEvent> findByAggregateTypeOrderByOccurredAtDescIdDesc(String aggregateType, Pageable pageable);

	List<AuditEvent> findByAggregateIdOrderByOccurredAtDescIdDesc(UUID aggregateId, Pageable pageable);

	List<AuditEvent> findByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
