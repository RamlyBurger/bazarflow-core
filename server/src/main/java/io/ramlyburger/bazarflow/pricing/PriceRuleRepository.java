package io.ramlyburger.bazarflow.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {

	boolean existsByRuleCode(String ruleCode);

	@EntityGraph(attributePaths = "priceBook")
	List<PriceRule> findByOrderByRuleCodeAsc();

	@Query("""
			select rule
			from PriceRule rule
			join fetch rule.priceBook book
			where rule.skuId = :skuId
				and rule.active = true
				and book.active = true
				and rule.validFrom <= :quoteDate
				and (rule.validUntil is null or rule.validUntil >= :quoteDate)
				and book.validFrom <= :quoteDate
				and (book.validUntil is null or book.validUntil >= :quoteDate)
				and rule.minQuantity <= :quantity
				and (rule.retailerId is null or rule.retailerId = :retailerId)
				and (rule.deliveryZone is null or rule.deliveryZone = :deliveryZone)
			""")
	List<PriceRule> findCandidateRules(
			@Param("skuId") UUID skuId,
			@Param("retailerId") UUID retailerId,
			@Param("deliveryZone") String deliveryZone,
			@Param("quantity") BigDecimal quantity,
			@Param("quoteDate") LocalDate quoteDate
	);
}
