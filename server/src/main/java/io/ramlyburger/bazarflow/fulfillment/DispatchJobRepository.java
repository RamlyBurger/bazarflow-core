package io.ramlyburger.bazarflow.fulfillment;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DispatchJobRepository extends JpaRepository<DispatchJob, UUID> {

	List<DispatchJob> findBySlaAtRiskTrueOrderByRequestedDeliveryDateAscDeliveryWindowEndAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select dispatchJob from DispatchJob dispatchJob where dispatchJob.id = :id")
	Optional<DispatchJob> findByIdForUpdate(@Param("id") UUID id);
}
