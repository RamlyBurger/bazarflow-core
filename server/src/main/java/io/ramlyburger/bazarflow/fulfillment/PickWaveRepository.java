package io.ramlyburger.bazarflow.fulfillment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PickWaveRepository extends JpaRepository<PickWave, UUID> {

	@Query("select distinct p from PickWave p left join fetch p.dispatchJobs order by p.plannedAt desc")
	List<PickWave> findAllWithDispatchJobsOrderByPlannedAtDesc();

	@EntityGraph(attributePaths = "dispatchJobs")
	@Query("select p from PickWave p where p.id = :id")
	Optional<PickWave> findWithDispatchJobsById(UUID id);
}
