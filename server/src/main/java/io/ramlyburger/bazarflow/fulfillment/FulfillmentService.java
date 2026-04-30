package io.ramlyburger.bazarflow.fulfillment;

import io.ramlyburger.bazarflow.common.AuditTrailEvent;
import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import io.ramlyburger.bazarflow.inventory.InventoryReservationService;
import io.ramlyburger.bazarflow.ordering.OrderFulfillmentService;
import io.ramlyburger.bazarflow.partner.OutletDeliveryWindow;
import io.ramlyburger.bazarflow.partner.OutletDeliveryWindowService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FulfillmentService {

	private static final DateTimeFormatter WAVE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
	private static final Duration DEFAULT_ROUTE_BUFFER = Duration.ofHours(2);

	private final PickWaveRepository pickWaveRepository;
	private final DispatchJobRepository dispatchJobRepository;
	private final InventoryReservationService inventoryReservationService;
	private final OrderFulfillmentService orderFulfillmentService;
	private final OutletDeliveryWindowService outletDeliveryWindowService;
	private final JdbcTemplate jdbcTemplate;
	private final ApplicationEventPublisher eventPublisher;

	FulfillmentService(
			PickWaveRepository pickWaveRepository,
			DispatchJobRepository dispatchJobRepository,
			InventoryReservationService inventoryReservationService,
			OrderFulfillmentService orderFulfillmentService,
			OutletDeliveryWindowService outletDeliveryWindowService,
			JdbcTemplate jdbcTemplate,
			ApplicationEventPublisher eventPublisher
	) {
		this.pickWaveRepository = pickWaveRepository;
		this.dispatchJobRepository = dispatchJobRepository;
		this.inventoryReservationService = inventoryReservationService;
		this.orderFulfillmentService = orderFulfillmentService;
		this.outletDeliveryWindowService = outletDeliveryWindowService;
		this.jdbcTemplate = jdbcTemplate;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	PickWaveResponse createPickWave(CreatePickWaveRequest request) {
		validateDeliveryDate(request.deliveryDate());
		String deliveryZone = normalizeDeliveryZone(request.deliveryZone());
		List<AcceptedOrderCandidate> candidates = findAcceptedOrderCandidates(request.deliveryDate(), deliveryZone);

		if (candidates.isEmpty()) {
			throw new ConflictException(
					"NO_ACCEPTED_ORDERS_READY",
					"No accepted orders are ready for the requested delivery date and zone"
			);
		}

		PickWave pickWave = PickWave.open(
				generateWaveNumber(request.deliveryDate(), deliveryZone),
				deliveryZone,
				request.deliveryDate()
		);
		Map<UUID, OutletDeliveryWindow> deliveryWindows = findDeliveryWindows(candidates);
		for (AcceptedOrderCandidate candidate : candidates) {
			OutletDeliveryWindow deliveryWindow = findDeliveryWindow(deliveryWindows, candidate.outletId());
			SlaEvaluation sla = evaluateSla(candidate.requestedDeliveryDate(), deliveryWindow.deliveryWindowEnd());
			pickWave.addDispatchJob(
					candidate.orderId(),
					candidate.orderNumber(),
					candidate.retailerId(),
					candidate.outletId(),
					candidate.requestedDeliveryDate(),
					deliveryWindow.deliveryWindowStart(),
					deliveryWindow.deliveryWindowEnd(),
					candidate.lineCount(),
					sla.atRisk(),
					sla.reason()
			);
		}

		PickWave savedPickWave = pickWaveRepository.save(pickWave);
		publishPickWaveCreated(savedPickWave);
		savedPickWave.dispatchJobs().forEach(this::publishDispatchPlanned);
		return toResponse(savedPickWave);
	}

	@Transactional(readOnly = true)
	List<PickWaveResponse> listPickWaves() {
		return pickWaveRepository.findAllWithDispatchJobsOrderByPlannedAtDesc()
				.stream()
				.map(FulfillmentService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	PickWaveResponse getPickWave(UUID pickWaveId) {
		return pickWaveRepository.findWithDispatchJobsById(pickWaveId)
				.map(FulfillmentService::toResponse)
				.orElseThrow(() -> new NotFoundException("PICK_WAVE_NOT_FOUND", "Pick wave was not found"));
	}

	@Transactional(readOnly = true)
	List<DispatchJobResponse> listSlaRiskJobs() {
		return dispatchJobRepository.findBySlaAtRiskTrueOrderByRequestedDeliveryDateAscDeliveryWindowEndAsc()
				.stream()
				.map(FulfillmentService::toDispatchJobResponse)
				.toList();
	}

	@Transactional
	DispatchJobResponse completeDispatchJob(UUID dispatchJobId) {
		DispatchJob dispatchJob = findDispatchJobForUpdate(dispatchJobId);
		if (!dispatchJob.isOpenForOutcome()) {
			throw new ConflictException(
					"DISPATCH_JOB_NOT_COMPLETABLE",
					"Only planned or in-progress dispatch jobs can be completed"
			);
		}

		inventoryReservationService.consumeForOrder(dispatchJob.orderId());
		orderFulfillmentService.markDelivered(dispatchJob.orderId());
		dispatchJob.complete();
		publishDispatchCompleted(dispatchJob);
		return toDispatchJobResponse(dispatchJob);
	}

	@Transactional
	DispatchJobResponse failDispatchJob(UUID dispatchJobId, FailDispatchJobRequest request) {
		DispatchJob dispatchJob = findDispatchJobForUpdate(dispatchJobId);
		if (!dispatchJob.isOpenForOutcome()) {
			throw new ConflictException(
					"DISPATCH_JOB_NOT_FAILABLE",
					"Only planned or in-progress dispatch jobs can be failed"
			);
		}

		String reason = normalizeFailureReason(request.reason());
		orderFulfillmentService.markDeliveryFailed(dispatchJob.orderId(), reason);
		dispatchJob.fail(reason);
		publishDispatchFailed(dispatchJob);
		return toDispatchJobResponse(dispatchJob);
	}

	private DispatchJob findDispatchJobForUpdate(UUID dispatchJobId) {
		return dispatchJobRepository.findByIdForUpdate(dispatchJobId)
				.orElseThrow(() -> new NotFoundException("DISPATCH_JOB_NOT_FOUND", "Dispatch job was not found"));
	}

	private List<AcceptedOrderCandidate> findAcceptedOrderCandidates(LocalDate deliveryDate, String deliveryZone) {
		return jdbcTemplate.query(
				"""
						select
						    o.id,
						    o.order_number,
						    o.retailer_id,
						    o.outlet_id,
						    o.delivery_zone,
						    o.requested_delivery_date,
						    count(ol.id)::int as line_count
						from ordering.orders o
						join ordering.order_lines ol on ol.order_id = o.id
						where o.status = 'ACCEPTED'
						  and o.requested_delivery_date = ?
						  and o.delivery_zone = ?
						  and not exists (
						      select 1
						      from fulfillment.dispatch_jobs existing_job
						      where existing_job.order_id = o.id
						  )
						group by
						    o.id,
						    o.order_number,
						    o.retailer_id,
						    o.outlet_id,
						    o.delivery_zone,
						    o.requested_delivery_date,
						    o.created_at
						order by o.created_at, o.order_number
						""",
				(rs, rowNum) -> new AcceptedOrderCandidate(
						rs.getObject("id", UUID.class),
						rs.getString("order_number"),
						rs.getObject("retailer_id", UUID.class),
						rs.getObject("outlet_id", UUID.class),
						rs.getObject("requested_delivery_date", LocalDate.class),
						rs.getInt("line_count")
				),
				deliveryDate,
				deliveryZone
		);
	}

	private Map<UUID, OutletDeliveryWindow> findDeliveryWindows(List<AcceptedOrderCandidate> candidates) {
		Set<UUID> outletIds = candidates.stream()
				.map(AcceptedOrderCandidate::outletId)
				.collect(Collectors.toSet());

		return outletDeliveryWindowService.findByOutletIds(outletIds);
	}

	private static OutletDeliveryWindow findDeliveryWindow(Map<UUID, OutletDeliveryWindow> deliveryWindows, UUID outletId) {
		OutletDeliveryWindow deliveryWindow = deliveryWindows.get(outletId);
		if (deliveryWindow == null) {
			throw new NotFoundException("OUTLET_NOT_FOUND", "Outlet delivery window was not found");
		}
		return deliveryWindow;
	}

	private SlaEvaluation evaluateSla(LocalDate deliveryDate, LocalTime deliveryWindowEnd) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		if (deliveryDate.isBefore(today)) {
			return new SlaEvaluation(true, "REQUESTED_DATE_PASSED");
		}

		if (deliveryDate.isEqual(today)) {
			LocalTime routeReadyTime = LocalTime.now(ZoneOffset.UTC).plus(DEFAULT_ROUTE_BUFFER);
			if (!routeReadyTime.isBefore(deliveryWindowEnd)) {
				return new SlaEvaluation(true, "DELIVERY_WINDOW_AT_RISK");
			}
		}

		return new SlaEvaluation(false, null);
	}

	private void publishPickWaveCreated(PickWave pickWave) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"fulfillment",
				"PICK_WAVE",
				pickWave.id(),
				"PICK_WAVE_CREATED",
				"Pick wave created",
				Map.of(
						"waveNumber", pickWave.waveNumber(),
						"deliveryZone", pickWave.deliveryZone(),
						"deliveryDate", pickWave.deliveryDate().toString(),
						"orderCount", Integer.toString(pickWave.orderCount()),
						"lineCount", Integer.toString(pickWave.lineCount())
				)
		));
	}

	private void publishDispatchPlanned(DispatchJob dispatchJob) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"fulfillment",
				"ORDER",
				dispatchJob.orderId(),
				"DISPATCH_PLANNED",
				"Dispatch job planned",
				Map.of(
						"dispatchJobId", dispatchJob.id().toString(),
						"orderNumber", dispatchJob.orderNumber(),
						"deliveryZone", dispatchJob.deliveryZone(),
						"requestedDeliveryDate", dispatchJob.requestedDeliveryDate().toString(),
						"slaAtRisk", Boolean.toString(dispatchJob.slaAtRisk())
				)
		));
	}

	private void publishDispatchCompleted(DispatchJob dispatchJob) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"fulfillment",
				"ORDER",
				dispatchJob.orderId(),
				"DISPATCH_COMPLETED",
				"Dispatch job completed",
				Map.of(
						"dispatchJobId", dispatchJob.id().toString(),
						"orderNumber", dispatchJob.orderNumber(),
						"status", dispatchJob.status().name(),
						"completedAt", dispatchJob.completedAt().toString()
				)
		));
	}

	private void publishDispatchFailed(DispatchJob dispatchJob) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"fulfillment",
				"ORDER",
				dispatchJob.orderId(),
				"DISPATCH_FAILED",
				"Dispatch job failed",
				Map.of(
						"dispatchJobId", dispatchJob.id().toString(),
						"orderNumber", dispatchJob.orderNumber(),
						"status", dispatchJob.status().name(),
						"failedAt", dispatchJob.failedAt().toString(),
						"reason", dispatchJob.failureReason()
				)
		));
	}

	private static PickWaveResponse toResponse(PickWave pickWave) {
		return new PickWaveResponse(
				pickWave.id(),
				pickWave.waveNumber(),
				pickWave.deliveryZone(),
				pickWave.deliveryDate(),
				pickWave.status(),
				pickWave.orderCount(),
				pickWave.lineCount(),
				pickWave.plannedAt(),
				pickWave.dispatchJobs()
						.stream()
						.sorted(Comparator
								.comparing(DispatchJob::deliveryWindowStart)
								.thenComparing(DispatchJob::orderNumber))
						.map(FulfillmentService::toDispatchJobResponse)
						.toList()
		);
	}

	private static DispatchJobResponse toDispatchJobResponse(DispatchJob dispatchJob) {
		return new DispatchJobResponse(
				dispatchJob.id(),
				dispatchJob.orderId(),
				dispatchJob.orderNumber(),
				dispatchJob.retailerId(),
				dispatchJob.outletId(),
				dispatchJob.deliveryZone(),
				dispatchJob.requestedDeliveryDate(),
				dispatchJob.deliveryWindowStart(),
				dispatchJob.deliveryWindowEnd(),
				dispatchJob.status(),
				dispatchJob.slaAtRisk(),
				dispatchJob.slaRiskReason(),
				dispatchJob.plannedAt(),
				dispatchJob.completedAt(),
				dispatchJob.failedAt(),
				dispatchJob.failureReason()
		);
	}

	private static void validateDeliveryDate(LocalDate deliveryDate) {
		if (deliveryDate.isBefore(LocalDate.now(ZoneOffset.UTC))) {
			throw new ConflictException("INVALID_PICK_WAVE_DATE", "Pick wave delivery date cannot be in the past");
		}
	}

	private static String normalizeDeliveryZone(String deliveryZone) {
		return deliveryZone.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeFailureReason(String reason) {
		return reason.trim();
	}

	private static String generateWaveNumber(LocalDate deliveryDate, String deliveryZone) {
		String zonePart = deliveryZone.replaceAll("[^A-Z0-9]", "");
		if (zonePart.isBlank()) {
			zonePart = "ZONE";
		}
		if (zonePart.length() > 10) {
			zonePart = zonePart.substring(0, 10);
		}

		String suffix = UUID.randomUUID().toString()
				.replace("-", "")
				.substring(0, 6)
				.toUpperCase(Locale.ROOT);
		return "PW-" + WAVE_DATE_FORMAT.format(deliveryDate) + "-" + zonePart + "-" + suffix;
	}

	private record AcceptedOrderCandidate(
			UUID orderId,
			String orderNumber,
			UUID retailerId,
			UUID outletId,
			LocalDate requestedDeliveryDate,
			int lineCount
	) {
	}

	private record SlaEvaluation(boolean atRisk, String reason) {
	}
}
