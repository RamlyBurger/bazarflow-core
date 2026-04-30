package io.ramlyburger.bazarflow.reporting;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReportingService {

	private static final int DASHBOARD_LIMIT = 8;
	private static final int RISK_LIMIT = 6;
	private static final int AUDIT_LIMIT = 6;
	private static final int THROUGHPUT_DAYS = 7;

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Autowired
	ReportingService(JdbcTemplate jdbcTemplate) {
		this(jdbcTemplate, Clock.systemUTC());
	}

	ReportingService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	ReportingDashboardResponse dashboard() {
		Instant checkedAt = clock.instant();
		LocalDate today = LocalDate.now(clock);

		return new ReportingDashboardResponse(
				checkedAt,
				findKpis(today),
				findWorkQueue(),
				findRisks(today),
				findThroughput(today),
				findDispatchBacklog(),
				findAuditTimeline()
		);
	}

	private List<DashboardKpiResponse> findKpis(LocalDate today) {
		long openOrders = count("""
				select count(*)
				from ordering.orders
				where status in ('SUBMITTED', 'ACCEPTED', 'DELIVERY_FAILED')
				""");
		BigDecimal reservedQuantity = sum("""
				select coalesce(sum(line.quantity), 0)
				from inventory.reservations reservation
				join inventory.reservation_lines line on line.reservation_id = reservation.id
				where reservation.status = 'ACTIVE'
				""");
		long slaRiskJobs = count("""
				select count(*)
				from fulfillment.dispatch_jobs
				where status in ('PLANNED', 'IN_PROGRESS')
				  and sla_at_risk = true
				""");
		long expiryRiskLots = count("""
				select count(*)
				from inventory.inventory_lots lot
				where lot.status = 'AVAILABLE'
				  and (lot.available_quantity + lot.reserved_quantity) > 0
				  and lot.expiry_date between ? and ?
				""", today, today.plusDays(7));

		return List.of(
				new DashboardKpiResponse("Open orders", Long.toString(openOrders), "Submitted, accepted, or failed", "neutral"),
				new DashboardKpiResponse("Reserved units", formatQuantity(reservedQuantity), "Active reservations", "success"),
				new DashboardKpiResponse("SLA risk", Long.toString(slaRiskJobs), "Open dispatch jobs", "warning"),
				new DashboardKpiResponse("Expiry risk lots", Long.toString(expiryRiskLots), "Next 7 days", "danger")
		);
	}

	private List<WorkQueueItemResponse> findWorkQueue() {
		return jdbcTemplate.query(
				"""
						select
						    o.id as order_id,
						    o.order_number,
						    outlet.name as outlet_name,
						    o.delivery_zone,
						    o.requested_delivery_date,
						    o.status as order_status,
						    coalesce(dispatch.status, 'UNPLANNED') as dispatch_status,
						    coalesce(sum(order_line.quantity), 0) as total_quantity,
						    coalesce(dispatch.sla_at_risk, false) as sla_at_risk,
						    dispatch.sla_risk_reason,
						    dispatch.delivery_window_start,
						    dispatch.delivery_window_end
						from ordering.orders o
						join partner.outlets outlet on outlet.id = o.outlet_id
						join ordering.order_lines order_line on order_line.order_id = o.id
						left join fulfillment.dispatch_jobs dispatch on dispatch.order_id = o.id
						where o.status in ('SUBMITTED', 'ACCEPTED', 'DELIVERY_FAILED')
						   or dispatch.status in ('PLANNED', 'IN_PROGRESS', 'FAILED')
						group by
						    o.id,
						    o.order_number,
						    outlet.name,
						    o.delivery_zone,
						    o.requested_delivery_date,
						    o.status,
						    dispatch.status,
						    dispatch.sla_at_risk,
						    dispatch.sla_risk_reason,
						    dispatch.delivery_window_start,
						    dispatch.delivery_window_end,
						    o.created_at
						order by
						    coalesce(dispatch.sla_at_risk, false) desc,
						    o.requested_delivery_date asc,
						    coalesce(dispatch.delivery_window_end, time '23:59:59') asc,
						    o.created_at asc
						limit ?
						""",
				(rs, rowNum) -> new WorkQueueItemResponse(
						rs.getObject("order_id", UUID.class),
						rs.getString("order_number"),
						rs.getString("outlet_name"),
						rs.getString("delivery_zone"),
						rs.getObject("requested_delivery_date", LocalDate.class),
						rs.getString("order_status"),
						rs.getString("dispatch_status"),
						rs.getBigDecimal("total_quantity"),
						rs.getBoolean("sla_at_risk"),
						rs.getString("sla_risk_reason"),
						rs.getObject("delivery_window_start", LocalTime.class),
						rs.getObject("delivery_window_end", LocalTime.class)
				),
				DASHBOARD_LIMIT
		);
	}

	private List<RiskItemResponse> findRisks(LocalDate today) {
		List<RiskItemResponse> risks = new ArrayList<>(findExpiryRisks(today));
		if (risks.size() < RISK_LIMIT) {
			risks.addAll(findSlaRisks(RISK_LIMIT - risks.size()));
		}
		return risks;
	}

	private List<RiskItemResponse> findExpiryRisks(LocalDate today) {
		return jdbcTemplate.query(
				"""
						select
						    sku.sku_code,
						    lot.lot_code,
						    lot.expiry_date
						from inventory.inventory_lots lot
						join catalog.skus sku on sku.id = lot.sku_id
						where lot.status = 'AVAILABLE'
						  and (lot.available_quantity + lot.reserved_quantity) > 0
						  and lot.expiry_date between ? and ?
						order by lot.expiry_date asc, lot.lot_code asc
						limit ?
						""",
				(rs, rowNum) -> {
					LocalDate expiryDate = rs.getObject("expiry_date", LocalDate.class);
					long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
					String lotCode = rs.getString("lot_code");
					return new RiskItemResponse(
							rs.getString("sku_code"),
							"Lot " + lotCode + " " + expiryText(daysUntilExpiry),
							daysUntilExpiry <= 2 ? "high" : "medium"
					);
				},
				today,
				today.plusDays(7),
				RISK_LIMIT
		);
	}

	private List<RiskItemResponse> findSlaRisks(int limit) {
		return jdbcTemplate.query(
				"""
						select
						    order_number,
						    delivery_zone,
						    requested_delivery_date,
						    delivery_window_end,
						    sla_risk_reason
						from fulfillment.dispatch_jobs
						where status in ('PLANNED', 'IN_PROGRESS')
						  and sla_at_risk = true
						order by requested_delivery_date asc, delivery_window_end asc, order_number asc
						limit ?
						""",
				(rs, rowNum) -> new RiskItemResponse(
						rs.getString("order_number"),
						"Zone " + rs.getString("delivery_zone") + " " + rs.getString("sla_risk_reason"),
						"high"
				),
				limit
		);
	}

	private List<ThroughputPointResponse> findThroughput(LocalDate today) {
		LocalDate startDate = today.minusDays(THROUGHPUT_DAYS - 1L);
		Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
		Map<LocalDate, ThroughputCounts> countsByDay = new LinkedHashMap<>();
		for (int index = 0; index < THROUGHPUT_DAYS; index++) {
			countsByDay.put(startDate.plusDays(index), new ThroughputCounts(0, 0));
		}

		List<ThroughputPointResponse> actualCounts = jdbcTemplate.query(
				"""
						select
						    changed_at::date as business_day,
						    sum(case when to_status = 'SUBMITTED' then 1 else 0 end)::int as submitted_orders,
						    sum(case when to_status = 'DELIVERED' then 1 else 0 end)::int as delivered_orders
						from ordering.order_status_history
						where changed_at >= ?
						group by changed_at::date
						order by changed_at::date
						""",
				(rs, rowNum) -> new ThroughputPointResponse(
						rs.getObject("business_day", LocalDate.class),
						rs.getInt("submitted_orders"),
						rs.getInt("delivered_orders")
				),
				Timestamp.from(startInstant)
		);
		for (ThroughputPointResponse count : actualCounts) {
			countsByDay.put(
					count.day(),
					new ThroughputCounts(count.submittedOrders(), count.deliveredOrders())
			);
		}

		return countsByDay.entrySet()
				.stream()
				.map(entry -> new ThroughputPointResponse(
						entry.getKey(),
						entry.getValue().submittedOrders(),
						entry.getValue().deliveredOrders()
				))
				.toList();
	}

	private List<DispatchBacklogResponse> findDispatchBacklog() {
		return jdbcTemplate.query(
				"""
						select
						    delivery_zone,
						    count(*)::int as open_jobs,
						    sum(case when sla_at_risk then 1 else 0 end)::int as at_risk_jobs
						from fulfillment.dispatch_jobs
						where status in ('PLANNED', 'IN_PROGRESS')
						group by delivery_zone
						order by delivery_zone asc
						""",
				(rs, rowNum) -> new DispatchBacklogResponse(
						rs.getString("delivery_zone"),
						rs.getInt("open_jobs"),
						rs.getInt("at_risk_jobs")
				)
		);
	}

	private List<AuditTimelineItemResponse> findAuditTimeline() {
		return jdbcTemplate.query(
				"""
						select
						    id,
						    source_module,
						    aggregate_type,
						    aggregate_id,
						    event_type,
						    message,
						    occurred_at
						from audit.audit_events
						order by occurred_at desc, id desc
						limit ?
						""",
				(rs, rowNum) -> new AuditTimelineItemResponse(
						rs.getObject("id", UUID.class),
						rs.getString("source_module"),
						rs.getString("aggregate_type"),
						rs.getObject("aggregate_id", UUID.class),
						rs.getString("event_type"),
						rs.getString("message"),
						rs.getTimestamp("occurred_at").toInstant()
				),
				AUDIT_LIMIT
		);
	}

	private long count(String sql, Object... args) {
		Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
		return value == null ? 0L : value;
	}

	private BigDecimal sum(String sql, Object... args) {
		BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String formatQuantity(BigDecimal quantity) {
		return quantity.stripTrailingZeros().toPlainString();
	}

	private static String expiryText(long daysUntilExpiry) {
		if (daysUntilExpiry == 0) {
			return "expires today";
		}
		if (daysUntilExpiry == 1) {
			return "expires in 1 day";
		}
		return "expires in " + daysUntilExpiry + " days";
	}

	private record ThroughputCounts(int submittedOrders, int deliveredOrders) {
	}
}
