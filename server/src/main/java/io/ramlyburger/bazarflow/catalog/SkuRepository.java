package io.ramlyburger.bazarflow.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SkuRepository extends JpaRepository<Sku, UUID> {

	boolean existsBySkuCode(String skuCode);

	@EntityGraph(attributePaths = "product")
	List<Sku> findByOrderBySkuCodeAsc();

	@Query("select sku from Sku sku join fetch sku.product where sku.id = :skuId")
	Optional<Sku> findWithProductById(@Param("skuId") UUID skuId);
}
