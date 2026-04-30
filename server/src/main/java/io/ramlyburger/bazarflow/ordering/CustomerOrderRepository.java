package io.ramlyburger.bazarflow.ordering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

	@Query("""
			select distinct customerOrder
			from CustomerOrder customerOrder
			left join fetch customerOrder.lines
			order by customerOrder.createdAt desc
			""")
	List<CustomerOrder> findAllWithLinesOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = "lines")
	Optional<CustomerOrder> findWithLinesById(UUID id);
}
