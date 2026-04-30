package io.ramlyburger.bazarflow.pricing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PriceBookRepository extends JpaRepository<PriceBook, UUID> {

	boolean existsByCode(String code);
}
