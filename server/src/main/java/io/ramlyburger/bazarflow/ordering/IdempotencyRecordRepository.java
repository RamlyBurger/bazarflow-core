package io.ramlyburger.bazarflow.ordering;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

	Optional<IdempotencyRecord> findByCommandTypeAndTargetIdAndIdempotencyKey(
			String commandType,
			UUID targetId,
			String idempotencyKey
	);
}
