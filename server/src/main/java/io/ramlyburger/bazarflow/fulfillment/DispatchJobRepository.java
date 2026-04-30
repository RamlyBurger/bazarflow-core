package io.ramlyburger.bazarflow.fulfillment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DispatchJobRepository extends JpaRepository<DispatchJob, UUID> {

	List<DispatchJob> findBySlaAtRiskTrueOrderByRequestedDeliveryDateAscDeliveryWindowEndAsc();
}
