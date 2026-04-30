package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

	boolean existsByOrderId(UUID orderId);

	@EntityGraph(attributePaths = "lines")
	List<InventoryReservation> findByOrderByReservedAtDesc();

	@EntityGraph(attributePaths = "lines")
	Optional<InventoryReservation> findWithLinesById(UUID reservationId);

	@EntityGraph(attributePaths = "lines")
	Optional<InventoryReservation> findWithLinesByOrderId(UUID orderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from InventoryReservation reservation where reservation.orderId = :orderId")
	Optional<InventoryReservation> findByOrderIdForUpdate(@Param("orderId") UUID orderId);
}
