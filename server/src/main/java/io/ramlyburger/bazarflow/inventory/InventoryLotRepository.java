package io.ramlyburger.bazarflow.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {

	boolean existsBySkuIdAndLotCode(UUID skuId, String lotCode);

	List<InventoryLot> findByOrderByExpiryDateAscLotCodeAsc();

	List<InventoryLot> findBySkuIdOrderByExpiryDateAscReceivedAtAsc(UUID skuId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select lot
			from InventoryLot lot
			where lot.skuId = :skuId
				and lot.status = :status
				and lot.availableQuantity > 0
				and lot.expiryDate >= :minimumExpiryDate
			order by lot.expiryDate asc, lot.receivedAt asc
			""")
	List<InventoryLot> findAvailableLotsForReservation(
			@Param("skuId") UUID skuId,
			@Param("status") LotStatus status,
			@Param("minimumExpiryDate") java.time.LocalDate minimumExpiryDate
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select lot
			from InventoryLot lot
			where lot.id in :lotIds
			""")
	List<InventoryLot> findAllByIdForUpdate(@Param("lotIds") Set<UUID> lotIds);
}
