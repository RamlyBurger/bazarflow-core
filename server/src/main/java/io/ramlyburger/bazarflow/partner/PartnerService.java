package io.ramlyburger.bazarflow.partner;

import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PartnerService {

	private static final String RETAILER_NOT_FOUND = "RETAILER_NOT_FOUND";

	private final RetailerRepository retailerRepository;
	private final OutletRepository outletRepository;

	PartnerService(RetailerRepository retailerRepository, OutletRepository outletRepository) {
		this.retailerRepository = retailerRepository;
		this.outletRepository = outletRepository;
	}

	@Transactional
	RetailerDetailsResponse createRetailer(CreateRetailerRequest request) {
		String registrationNumber = normalizeRegistrationNumber(request.registrationNumber());

		if (registrationNumber != null && retailerRepository.existsByRegistrationNumber(registrationNumber)) {
			throw new ConflictException(
					"RETAILER_REGISTRATION_ALREADY_EXISTS",
					"Retailer registration number already exists"
			);
		}

		Retailer retailer = Retailer.create(
				normalizeRequired(request.legalName()),
				normalizeOptional(request.tradingName()),
				registrationNumber
		);

		return toDetails(retailerRepository.save(retailer), List.of());
	}

	@Transactional(readOnly = true)
	List<RetailerSummaryResponse> listRetailers() {
		return retailerRepository.findAll(Sort.by(Sort.Direction.ASC, "legalName"))
				.stream()
				.map(PartnerService::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	RetailerDetailsResponse getRetailer(UUID retailerId) {
		Retailer retailer = findRetailer(retailerId);
		List<Outlet> outlets = outletRepository.findByRetailerIdOrderByNameAsc(retailerId);
		return toDetails(retailer, outlets);
	}

	@Transactional
	OutletResponse createOutlet(UUID retailerId, CreateOutletRequest request) {
		validateDeliveryWindow(request.deliveryWindowStart(), request.deliveryWindowEnd());

		Retailer retailer = findRetailer(retailerId);
		Outlet outlet = Outlet.create(
				retailer,
				normalizeRequired(request.name()),
				normalizeRequired(request.deliveryZone()).toUpperCase(Locale.ROOT),
				normalizeRequired(request.addressLine1()),
				normalizeRequired(request.city()),
				normalizeRequired(request.state()),
				normalizeRequired(request.postalCode()),
				request.deliveryWindowStart(),
				request.deliveryWindowEnd()
		);

		return toOutlet(outletRepository.save(outlet));
	}

	@Transactional
	RetailerDetailsResponse updateCreditStatus(UUID retailerId, UpdateCreditStatusRequest request) {
		Retailer retailer = findRetailer(retailerId);
		retailer.changeCreditStatus(request.creditStatus());
		List<Outlet> outlets = outletRepository.findByRetailerIdOrderByNameAsc(retailerId);
		return toDetails(retailer, outlets);
	}

	private Retailer findRetailer(UUID retailerId) {
		return retailerRepository.findById(retailerId)
				.orElseThrow(() -> new NotFoundException(RETAILER_NOT_FOUND, "Retailer was not found"));
	}

	private static void validateDeliveryWindow(LocalTime start, LocalTime end) {
		if (start != null && end != null && !start.isBefore(end)) {
			throw new ConflictException(
					"INVALID_DELIVERY_WINDOW",
					"Delivery window start must be before delivery window end"
			);
		}
	}

	private static String normalizeRequired(String value) {
		return value.trim();
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private static String normalizeRegistrationNumber(String value) {
		String normalized = normalizeOptional(value);
		return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
	}

	private static RetailerSummaryResponse toSummary(Retailer retailer) {
		return new RetailerSummaryResponse(
				retailer.id(),
				retailer.legalName(),
				retailer.tradingName(),
				retailer.registrationNumber(),
				retailer.creditStatus(),
				retailer.createdAt(),
				retailer.updatedAt()
		);
	}

	private static RetailerDetailsResponse toDetails(Retailer retailer, List<Outlet> outlets) {
		return new RetailerDetailsResponse(
				retailer.id(),
				retailer.legalName(),
				retailer.tradingName(),
				retailer.registrationNumber(),
				retailer.creditStatus(),
				retailer.createdAt(),
				retailer.updatedAt(),
				outlets.stream()
						.sorted(Comparator.comparing(Outlet::name))
						.map(PartnerService::toOutlet)
						.toList()
		);
	}

	private static OutletResponse toOutlet(Outlet outlet) {
		return new OutletResponse(
				outlet.id(),
				outlet.name(),
				outlet.deliveryZone(),
				outlet.addressLine1(),
				outlet.city(),
				outlet.state(),
				outlet.postalCode(),
				outlet.deliveryWindowStart(),
				outlet.deliveryWindowEnd(),
				outlet.active()
		);
	}
}
