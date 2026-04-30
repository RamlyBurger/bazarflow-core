package io.ramlyburger.bazarflow.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

	boolean existsByOrderId(UUID orderId);

	@EntityGraph(attributePaths = "lines")
	List<InventoryReservation> findByOrderByReservedAtDesc();

	@EntityGraph(attributePaths = "lines")
	Optional<InventoryReservation> findWithLinesById(UUID reservationId);

	@EntityGraph(attributePaths = "lines")
	Optional<InventoryReservation> findWithLinesByOrderId(UUID orderId);
}
