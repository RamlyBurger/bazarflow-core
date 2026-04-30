package io.ramlyburger.bazarflow.partner;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutletRepository extends JpaRepository<Outlet, UUID> {

	List<Outlet> findByRetailerIdOrderByNameAsc(UUID retailerId);

	List<Outlet> findByIdIn(Collection<UUID> outletIds);
}
