package io.ramlyburger.bazarflow.catalog;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {

	boolean existsByNameIgnoreCase(String name);
}
