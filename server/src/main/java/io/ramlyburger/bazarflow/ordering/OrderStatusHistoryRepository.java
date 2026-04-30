package io.ramlyburger.bazarflow.ordering;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

	List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
