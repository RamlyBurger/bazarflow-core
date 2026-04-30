package io.ramlyburger.bazarflow.ordering;

import io.ramlyburger.bazarflow.common.BusinessException;
import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import io.ramlyburger.bazarflow.inventory.InventoryReservationCommand;
import io.ramlyburger.bazarflow.inventory.InventoryReservationItemCommand;
import io.ramlyburger.bazarflow.inventory.InventoryReservationService;
import io.ramlyburger.bazarflow.pricing.PriceQuoteCommand;
import io.ramlyburger.bazarflow.pricing.PriceQuoteItemCommand;
import io.ramlyburger.bazarflow.pricing.PriceQuoteLineResponse;
import io.ramlyburger.bazarflow.pricing.PriceQuoteResponse;
import io.ramlyburger.bazarflow.pricing.PricingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderService {

	private static final String SUBMIT_ORDER = "SUBMIT_ORDER";
	private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final CustomerOrderRepository orderRepository;
	private final OrderStatusHistoryRepository statusHistoryRepository;
	private final IdempotencyRecordRepository idempotencyRecordRepository;
	private final InventoryReservationService inventoryReservationService;
	private final PricingService pricingService;
	private final JdbcTemplate jdbcTemplate;

	OrderService(
			CustomerOrderRepository orderRepository,
			OrderStatusHistoryRepository statusHistoryRepository,
			IdempotencyRecordRepository idempotencyRecordRepository,
			InventoryReservationService inventoryReservationService,
			PricingService pricingService,
			JdbcTemplate jdbcTemplate
	) {
		this.orderRepository = orderRepository;
		this.statusHistoryRepository = statusHistoryRepository;
		this.idempotencyRecordRepository = idempotencyRecordRepository;
		this.inventoryReservationService = inventoryReservationService;
		this.pricingService = pricingService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	OrderResponse createDraft(CreateOrderRequest request) {
		validateRequestedDeliveryDate(request.requestedDeliveryDate());
		validateDistinctSkus(request.lines());

		OutletSnapshot outlet = findOutlet(request.retailerId(), request.outletId());
		List<PriceQuoteItemCommand> quoteItems = request.lines()
				.stream()
				.map(line -> {
					validateSkuExists(line.skuId());
					return new PriceQuoteItemCommand(line.skuId(), line.quantity());
				})
				.toList();
		PriceQuoteResponse quote = pricingService.quote(
				new PriceQuoteCommand(request.retailerId(), outlet.deliveryZone(), quoteItems)
		);

		CustomerOrder order = CustomerOrder.draft(
				generateOrderNumber(),
				request.retailerId(),
				request.outletId(),
				outlet.deliveryZone(),
				request.requestedDeliveryDate(),
				quote.currency(),
				quote.subtotal()
		);

		for (int index = 0; index < quote.lines().size(); index++) {
			PriceQuoteLineResponse quotedLine = quote.lines().get(index);
			order.addLine(
					index + 1,
					quotedLine.skuId(),
					quotedLine.quantity(),
					quotedLine.unitPrice(),
					quotedLine.lineTotal()
			);
		}

		CustomerOrder savedOrder = orderRepository.save(order);
		statusHistoryRepository.save(OrderStatusHistory.created(savedOrder.id(), "Draft created", "system"));
		return toResponse(savedOrder);
	}

	@Transactional(readOnly = true)
	List<OrderSummaryResponse> listOrders() {
		return orderRepository.findAllWithLinesOrderByCreatedAtDesc()
				.stream()
				.map(OrderService::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	OrderResponse getOrder(UUID orderId) {
		return toResponse(findOrder(orderId));
	}

	@Transactional
	OrderResponse submit(UUID orderId, String idempotencyKey) {
		String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

		return idempotencyRecordRepository.findByCommandTypeAndTargetIdAndIdempotencyKey(
						SUBMIT_ORDER,
						orderId,
						normalizedKey
				)
				.map(record -> toResponse(findOrder(record.responseOrderId())))
				.orElseGet(() -> submitFirstAttempt(orderId, normalizedKey));
	}

	@Transactional(readOnly = true)
	List<OrderTimelineEntryResponse> getTimeline(UUID orderId) {
		if (!orderRepository.existsById(orderId)) {
			throw new NotFoundException("ORDER_NOT_FOUND", "Order was not found");
		}

		return statusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
				.stream()
				.map(OrderService::toTimelineEntry)
				.toList();
	}

	private OrderResponse submitFirstAttempt(UUID orderId, String idempotencyKey) {
		CustomerOrder order = findOrder(orderId);
		if (order.status() != OrderStatus.DRAFT) {
			throw new ConflictException("ORDER_NOT_SUBMITTABLE", "Only draft orders can be submitted");
		}

		validateRetailerCanSubmit(order.retailerId());
		inventoryReservationService.reserve(new InventoryReservationCommand(
				order.id(),
				order.requestedDeliveryDate(),
				order.lines()
						.stream()
						.map(line -> new InventoryReservationItemCommand(line.skuId(), line.quantity()))
						.toList()
		));
		order.submit();
		statusHistoryRepository.save(OrderStatusHistory.transition(
				order.id(),
				OrderStatus.DRAFT,
				OrderStatus.SUBMITTED,
				"Order submitted",
				"system"
		));
		idempotencyRecordRepository.save(IdempotencyRecord.success(
				idempotencyKey,
				SUBMIT_ORDER,
				order.id(),
				order.id()
		));

		return toResponse(order);
	}

	private CustomerOrder findOrder(UUID orderId) {
		return orderRepository.findWithLinesById(orderId)
				.orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order was not found"));
	}

	private OutletSnapshot findOutlet(UUID retailerId, UUID outletId) {
		List<OutletSnapshot> outlets = jdbcTemplate.query(
				"""
						select o.id, o.delivery_zone
						from partner.outlets o
						join partner.retailers r on r.id = o.retailer_id
						where o.id = ? and r.id = ? and o.active = true
						""",
				(rs, rowNum) -> new OutletSnapshot(
						rs.getObject("id", UUID.class),
						rs.getString("delivery_zone")
				),
				outletId,
				retailerId
		);

		if (outlets.isEmpty()) {
			throw new NotFoundException("OUTLET_NOT_FOUND", "Outlet was not found for retailer");
		}

		return outlets.get(0);
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

	private void validateRetailerCanSubmit(UUID retailerId) {
		List<String> statuses = jdbcTemplate.queryForList(
				"select credit_status from partner.retailers where id = ?",
				String.class,
				retailerId
		);

		if (statuses.isEmpty()) {
			throw new NotFoundException("RETAILER_NOT_FOUND", "Retailer was not found");
		}

		if ("BLOCKED".equals(statuses.get(0))) {
			throw new ConflictException("RETAILER_CREDIT_BLOCKED", "Blocked retailers cannot submit orders");
		}
	}

	private static void validateRequestedDeliveryDate(LocalDate requestedDeliveryDate) {
		if (requestedDeliveryDate.isBefore(LocalDate.now(ZoneOffset.UTC))) {
			throw new ConflictException("INVALID_REQUESTED_DELIVERY_DATE", "Requested delivery date cannot be in the past");
		}
	}

	private static void validateDistinctSkus(List<CreateOrderLineRequest> lines) {
		Set<UUID> skuIds = new HashSet<>();
		for (CreateOrderLineRequest line : lines) {
			if (!skuIds.add(line.skuId())) {
				throw new ConflictException("DUPLICATE_ORDER_SKU", "Order lines must not contain duplicate SKUs");
			}
		}
	}

	private static String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new BusinessException(
					"IDEMPOTENCY_KEY_REQUIRED",
					HttpStatus.BAD_REQUEST,
					"Idempotency-Key header is required"
			);
		}

		String normalized = idempotencyKey.trim();
		if (normalized.length() > 120) {
			throw new BusinessException(
					"IDEMPOTENCY_KEY_TOO_LONG",
					HttpStatus.BAD_REQUEST,
					"Idempotency-Key header must be 120 characters or fewer"
			);
		}

		return normalized;
	}

	private static String generateOrderNumber() {
		String date = ORDER_DATE_FORMAT.format(LocalDate.now(ZoneOffset.UTC));
		String suffix = UUID.randomUUID().toString()
				.replace("-", "")
				.substring(0, 8)
				.toUpperCase(Locale.ROOT);
		return "BF-" + date + "-" + suffix;
	}

	private static OrderResponse toResponse(CustomerOrder order) {
		List<OrderLineResponse> lines = order.lines()
				.stream()
				.sorted(Comparator.comparingInt(OrderLine::lineNumber))
				.map(OrderService::toLineResponse)
				.toList();

		return new OrderResponse(
				order.id(),
				order.orderNumber(),
				order.retailerId(),
				order.outletId(),
				order.deliveryZone(),
				order.requestedDeliveryDate(),
				order.status(),
				order.currency(),
				order.subtotal(),
				order.submittedAt(),
				order.createdAt(),
				order.updatedAt(),
				lines
		);
	}

	private static OrderSummaryResponse toSummary(CustomerOrder order) {
		return new OrderSummaryResponse(
				order.id(),
				order.orderNumber(),
				order.retailerId(),
				order.outletId(),
				order.deliveryZone(),
				order.requestedDeliveryDate(),
				order.status(),
				order.currency(),
				order.subtotal(),
				order.lines().size(),
				order.submittedAt(),
				order.createdAt(),
				order.updatedAt()
		);
	}

	private static OrderLineResponse toLineResponse(OrderLine line) {
		return new OrderLineResponse(
				line.id(),
				line.lineNumber(),
				line.skuId(),
				line.quantity(),
				line.unitPrice(),
				line.lineTotal()
		);
	}

	private static OrderTimelineEntryResponse toTimelineEntry(OrderStatusHistory history) {
		return new OrderTimelineEntryResponse(
				history.id(),
				history.fromStatus(),
				history.toStatus(),
				history.reason(),
				history.changedBy(),
				history.changedAt()
		);
	}

	private record OutletSnapshot(UUID outletId, String deliveryZone) {
	}
}
