package io.ramlyburger.bazarflow.pricing;

import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

	private static final String PRICE_BOOK_NOT_FOUND = "PRICE_BOOK_NOT_FOUND";

	private final PriceBookRepository priceBookRepository;
	private final PriceRuleRepository priceRuleRepository;
	private final JdbcTemplate jdbcTemplate;

	PricingService(
			PriceBookRepository priceBookRepository,
			PriceRuleRepository priceRuleRepository,
			JdbcTemplate jdbcTemplate
	) {
		this.priceBookRepository = priceBookRepository;
		this.priceRuleRepository = priceRuleRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	PriceBookResponse createPriceBook(CreatePriceBookRequest request) {
		validateDateRange(request.validFrom(), request.validUntil());

		String code = normalizeCode(request.code());
		if (priceBookRepository.existsByCode(code)) {
			throw new ConflictException("PRICE_BOOK_CODE_ALREADY_EXISTS", "Price book code already exists");
		}

		PriceBook priceBook = PriceBook.create(
				code,
				normalizeRequired(request.name()),
				normalizeCurrency(request.currency()),
				request.validFrom(),
				request.validUntil()
		);

		return toPriceBook(priceBookRepository.save(priceBook));
	}

	@Transactional(readOnly = true)
	List<PriceBookResponse> listPriceBooks() {
		return priceBookRepository.findAll(Sort.by(Sort.Direction.ASC, "code"))
				.stream()
				.map(PricingService::toPriceBook)
				.toList();
	}

	@Transactional
	PriceRuleResponse createRule(CreatePriceRuleRequest request) {
		validateDateRange(request.validFrom(), request.validUntil());
		validateSkuExists(request.skuId());

		if (request.retailerId() != null) {
			validateRetailerExists(request.retailerId());
		}

		String ruleCode = normalizeCode(request.ruleCode());
		if (priceRuleRepository.existsByRuleCode(ruleCode)) {
			throw new ConflictException("PRICE_RULE_CODE_ALREADY_EXISTS", "Price rule code already exists");
		}

		PriceBook priceBook = priceBookRepository.findById(request.priceBookId())
				.orElseThrow(() -> new NotFoundException(PRICE_BOOK_NOT_FOUND, "Price book was not found"));

		PriceRule rule = PriceRule.create(
				priceBook,
				ruleCode,
				normalizeOptional(request.description()),
				request.skuId(),
				request.retailerId(),
				normalizeOptionalCode(request.deliveryZone()),
				request.minQuantity(),
				request.unitPrice().setScale(2, RoundingMode.HALF_UP),
				request.priority(),
				request.validFrom(),
				request.validUntil()
		);

		return toPriceRule(priceRuleRepository.save(rule));
	}

	@Transactional(readOnly = true)
	List<PriceRuleResponse> listRules() {
		return priceRuleRepository.findByOrderByRuleCodeAsc()
				.stream()
				.map(PricingService::toPriceRule)
				.toList();
	}

	@Transactional(readOnly = true)
	public PriceQuoteResponse quote(PriceQuoteCommand command) {
		validateRetailerExists(command.retailerId());
		String deliveryZone = normalizeCode(command.deliveryZone());
		LocalDate quoteDate = LocalDate.now(ZoneOffset.UTC);

		List<PriceQuoteLineResponse> lines = command.items()
				.stream()
				.map(item -> quoteLine(command.retailerId(), deliveryZone, item, quoteDate))
				.toList();

		String currency = resolveCurrency(lines);
		BigDecimal subtotal = lines.stream()
				.map(PriceQuoteLineResponse::lineTotal)
				.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
		int appliedRuleCount = lines.stream()
				.mapToInt(line -> line.appliedRules().size())
				.sum();

		return new PriceQuoteResponse(command.retailerId(), deliveryZone, currency, lines, subtotal, appliedRuleCount);
	}

	private PriceQuoteLineResponse quoteLine(
			UUID retailerId,
			String deliveryZone,
			PriceQuoteItemCommand item,
			LocalDate quoteDate
	) {
		validateSkuExists(item.skuId());

		PriceRule rule = priceRuleRepository.findCandidateRules(
						item.skuId(),
						retailerId,
						deliveryZone,
						item.quantity(),
						quoteDate
				)
				.stream()
				.sorted(priceRuleComparator())
				.findFirst()
				.orElseThrow(() -> new ConflictException(
						"PRICE_RULE_NOT_FOUND",
						"No active price rule matched the quote line"
				));

		BigDecimal unitPrice = rule.unitPrice().setScale(2, RoundingMode.HALF_UP);
		BigDecimal lineTotal = unitPrice.multiply(item.quantity()).setScale(2, RoundingMode.HALF_UP);
		AppliedPriceRuleResponse appliedRule = new AppliedPriceRuleResponse(
				rule.id(),
				rule.ruleCode(),
				rule.description(),
				rule.priority()
		);

		return new PriceQuoteLineResponse(
				item.skuId(),
				item.quantity(),
				unitPrice,
				rule.priceBook().currency(),
				lineTotal,
				List.of(appliedRule)
		);
	}

	private Comparator<PriceRule> priceRuleComparator() {
		return Comparator
				.comparingInt(PriceRule::priority).reversed()
				.thenComparing(Comparator.comparingInt(PriceRule::specificity).reversed())
				.thenComparing(Comparator.comparing(PriceRule::minQuantity).reversed())
				.thenComparing(PriceRule::createdAt);
	}

	private static String resolveCurrency(List<PriceQuoteLineResponse> lines) {
		String currency = null;

		for (PriceQuoteLineResponse line : lines) {
			if (currency != null && !currency.equals(line.currency())) {
				throw new ConflictException("PRICE_CURRENCY_MISMATCH", "Matched price rules use different currencies");
			}
			currency = line.currency();
		}

		return currency;
	}

	private void validateSkuExists(UUID skuId) {
		Boolean exists = jdbcTemplate.queryForObject(
				"select exists(select 1 from catalog.skus where id = ? and status = 'ACTIVE')",
				Boolean.class,
				skuId
		);

		if (!Boolean.TRUE.equals(exists)) {
			throw new NotFoundException("SKU_NOT_FOUND", "SKU was not found");
		}
	}

	private void validateRetailerExists(UUID retailerId) {
		Boolean exists = jdbcTemplate.queryForObject(
				"select exists(select 1 from partner.retailers where id = ?)",
				Boolean.class,
				retailerId
		);

		if (!Boolean.TRUE.equals(exists)) {
			throw new NotFoundException("RETAILER_NOT_FOUND", "Retailer was not found");
		}
	}

	private static void validateDateRange(LocalDate validFrom, LocalDate validUntil) {
		if (validUntil != null && validUntil.isBefore(validFrom)) {
			throw new ConflictException("INVALID_VALIDITY_RANGE", "Valid-until date must not be before valid-from date");
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

	private static String normalizeCode(String value) {
		return normalizeRequired(value).toUpperCase(Locale.ROOT);
	}

	private static String normalizeOptionalCode(String value) {
		String normalized = normalizeOptional(value);
		return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
	}

	private static String normalizeCurrency(String value) {
		return normalizeCode(value);
	}

	private static PriceBookResponse toPriceBook(PriceBook priceBook) {
		return new PriceBookResponse(
				priceBook.id(),
				priceBook.code(),
				priceBook.name(),
				priceBook.currency(),
				priceBook.active(),
				priceBook.validFrom(),
				priceBook.validUntil(),
				priceBook.createdAt(),
				priceBook.updatedAt()
		);
	}

	private static PriceRuleResponse toPriceRule(PriceRule rule) {
		PriceBook priceBook = rule.priceBook();
		return new PriceRuleResponse(
				rule.id(),
				priceBook.id(),
				priceBook.code(),
				rule.ruleCode(),
				rule.description(),
				rule.skuId(),
				rule.retailerId(),
				rule.deliveryZone(),
				rule.minQuantity(),
				rule.unitPrice(),
				priceBook.currency(),
				rule.priority(),
				rule.active(),
				rule.validFrom(),
				rule.validUntil(),
				rule.createdAt(),
				rule.updatedAt()
		);
	}
}
