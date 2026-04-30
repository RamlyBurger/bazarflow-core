package io.ramlyburger.bazarflow.partner;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RetailerRepository extends JpaRepository<Retailer, UUID> {

	boolean existsByRegistrationNumber(String registrationNumber);
}
