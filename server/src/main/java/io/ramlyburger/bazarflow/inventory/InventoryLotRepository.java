package io.ramlyburger.bazarflow.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {

	boolean existsBySkuIdAndLotCode(UUID skuId, String lotCode);

	List<InventoryLot> findByOrderByExpiryDateAscLotCodeAsc();

	List<InventoryLot> findBySkuIdOrderByExpiryDateAscReceivedAtAsc(UUID skuId);
}
