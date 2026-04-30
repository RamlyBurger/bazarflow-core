package io.ramlyburger.bazarflow.reporting;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportingApiIntegrationTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void rejectsUnauthenticatedReportingRequests() throws Exception {
		mockMvc.perform(get("/api/reporting/dashboard"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void returnsDashboardReadModel() throws Exception {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		SeedIds ids = seedOperationalData(today);

		mockMvc.perform(get("/api/reporting/dashboard"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkedAt", notNullValue()))
				.andExpect(jsonPath("$.kpis", hasSize(4)))
				.andExpect(jsonPath("$.kpis[0].label").value("Open orders"))
				.andExpect(jsonPath("$.kpis[0].value").value("1"))
				.andExpect(jsonPath("$.kpis[1].label").value("Reserved units"))
				.andExpect(jsonPath("$.kpis[1].value").value("2"))
				.andExpect(jsonPath("$.kpis[2].value").value("1"))
				.andExpect(jsonPath("$.kpis[3].value").value("1"))
				.andExpect(jsonPath("$.workQueue", hasSize(1)))
				.andExpect(jsonPath("$.workQueue[0].orderId").value(ids.orderId().toString()))
				.andExpect(jsonPath("$.workQueue[0].orderNumber").value("BF-REPORT-001"))
				.andExpect(jsonPath("$.workQueue[0].outletName").value("Reporting Outlet"))
				.andExpect(jsonPath("$.workQueue[0].dispatchStatus").value("PLANNED"))
				.andExpect(jsonPath("$.workQueue[0].totalQuantity").value(2.0))
				.andExpect(jsonPath("$.workQueue[0].slaAtRisk").value(true))
				.andExpect(jsonPath("$.risks", hasSize(2)))
				.andExpect(jsonPath("$.risks[0].label").value("REPORT-SKU-001"))
				.andExpect(jsonPath("$.risks[0].severity").value("high"))
				.andExpect(jsonPath("$.risks[1].label").value("BF-REPORT-001"))
				.andExpect(jsonPath("$.dispatchBacklog", hasSize(1)))
				.andExpect(jsonPath("$.dispatchBacklog[0].deliveryZone").value("CENTRAL"))
				.andExpect(jsonPath("$.dispatchBacklog[0].openJobs").value(1))
				.andExpect(jsonPath("$.dispatchBacklog[0].atRiskJobs").value(1))
				.andExpect(jsonPath("$.throughput", hasSize(7)))
				.andExpect(jsonPath("$.throughput[6].submittedOrders").value(1))
				.andExpect(jsonPath("$.auditTimeline", hasSize(1)))
				.andExpect(jsonPath("$.auditTimeline[0].eventType").value("DISPATCH_PLANNED"));
	}

	private SeedIds seedOperationalData(LocalDate today) {
		Instant now = Instant.now();
		UUID retailerId = UUID.randomUUID();
		UUID outletId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID skuId = UUID.randomUUID();
		UUID lotId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		UUID pickWaveId = UUID.randomUUID();
		UUID dispatchJobId = UUID.randomUUID();

		jdbcTemplate.update(
				"""
						insert into partner.retailers (
						    id, legal_name, trading_name, registration_number, credit_status, created_at, updated_at
						)
						values (?, ?, ?, ?, 'ACTIVE', ?, ?)
						""",
				retailerId,
				"Reporting Retailer",
				"Reporting Retailer",
				"REPORT-REG-001",
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into partner.outlets (
						    id, retailer_id, name, delivery_zone, address_line_1, city, state, postal_code,
						    delivery_window_start, delivery_window_end, active, created_at, updated_at
						)
						values (?, ?, ?, 'CENTRAL', '1 Report Road', 'Report City', 'Report State', '10000',
						        ?, ?, true, ?, ?)
						""",
				outletId,
				retailerId,
				"Reporting Outlet",
				LocalTime.of(9, 0),
				LocalTime.of(10, 0),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into catalog.products (
						    id, name, category, description, metadata, created_at, updated_at
						)
						values (?, 'Reporting Product', 'Reporting', 'Reporting test product', '{}'::jsonb, ?, ?)
						""",
				productId,
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into catalog.skus (
						    id, product_id, sku_code, name, unit_of_measure, storage_class, shelf_life_days,
						    status, created_at, updated_at
						)
						values (?, ?, 'REPORT-SKU-001', 'Reporting SKU', 'PACK', 'FROZEN', 180, 'ACTIVE', ?, ?)
						""",
				skuId,
				productId,
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into inventory.inventory_lots (
						    id, sku_id, lot_code, warehouse_code, received_quantity, available_quantity,
						    reserved_quantity, dispatched_quantity, expiry_date, status, received_at, created_at, updated_at
						)
						values (?, ?, 'REPORT-LOT-001', 'MAIN-WAREHOUSE', ?, ?, ?, ?, ?, 'AVAILABLE', ?, ?, ?)
						""",
				lotId,
				skuId,
				new BigDecimal("5.000"),
				new BigDecimal("3.000"),
				new BigDecimal("2.000"),
				BigDecimal.ZERO,
				today.plusDays(2),
				timestamp(now),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into ordering.orders (
						    id, order_number, retailer_id, outlet_id, delivery_zone, requested_delivery_date,
						    status, currency, subtotal, submitted_at, created_at, updated_at
						)
						values (?, 'BF-REPORT-001', ?, ?, 'CENTRAL', ?, 'ACCEPTED', 'MYR', ?, ?, ?, ?)
						""",
				orderId,
				retailerId,
				outletId,
				today,
				new BigDecimal("20.00"),
				timestamp(now),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into ordering.order_lines (
						    id, order_id, line_number, sku_id, quantity, unit_price, line_total, created_at
						)
						values (?, ?, 1, ?, ?, ?, ?, ?)
						""",
				UUID.randomUUID(),
				orderId,
				skuId,
				new BigDecimal("2.000"),
				new BigDecimal("10.00"),
				new BigDecimal("20.00"),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into ordering.order_status_history (
						    id, order_id, from_status, to_status, reason, changed_by, changed_at
						)
						values (?, ?, 'DRAFT', 'SUBMITTED', 'Order submitted', 'system', ?)
						""",
				UUID.randomUUID(),
				orderId,
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into inventory.reservations (
						    id, order_id, required_by_date, status, reserved_at, expires_at, created_at, updated_at
						)
						values (?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
						""",
				reservationId,
				orderId,
				today,
				timestamp(now),
				timestamp(now.plusSeconds(14_400)),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into inventory.reservation_lines (
						    id, reservation_id, lot_id, sku_id, lot_code, warehouse_code, quantity, expiry_date, created_at
						)
						values (?, ?, ?, ?, 'REPORT-LOT-001', 'MAIN-WAREHOUSE', ?, ?, ?)
						""",
				UUID.randomUUID(),
				reservationId,
				lotId,
				skuId,
				new BigDecimal("2.000"),
				today.plusDays(2),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into fulfillment.pick_waves (
						    id, wave_number, delivery_zone, delivery_date, status, order_count, line_count,
						    planned_at, created_at, updated_at
						)
						values (?, 'PW-REPORT-001', 'CENTRAL', ?, 'OPEN', 1, 1, ?, ?, ?)
						""",
				pickWaveId,
				today,
				timestamp(now),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into fulfillment.dispatch_jobs (
						    id, pick_wave_id, order_id, order_number, retailer_id, outlet_id, delivery_zone,
						    requested_delivery_date, delivery_window_start, delivery_window_end, status, sla_at_risk,
						    sla_risk_reason, planned_at, created_at, updated_at
						)
						values (?, ?, ?, 'BF-REPORT-001', ?, ?, 'CENTRAL', ?, ?, ?, 'PLANNED', true,
						        'DELIVERY_WINDOW_AT_RISK', ?, ?, ?)
						""",
				dispatchJobId,
				pickWaveId,
				orderId,
				retailerId,
				outletId,
				today,
				LocalTime.of(9, 0),
				LocalTime.of(10, 0),
				timestamp(now),
				timestamp(now),
				timestamp(now)
		);
		jdbcTemplate.update(
				"""
						insert into audit.audit_events (
						    id, source_module, aggregate_type, aggregate_id, event_type, message, actor,
						    correlation_id, details, occurred_at, created_at
						)
						values (?, 'fulfillment', 'ORDER', ?, 'DISPATCH_PLANNED', 'Dispatch job planned',
						        'system', 'reporting-test', '{}', ?, ?)
						""",
				UUID.randomUUID(),
				orderId,
				timestamp(now),
				timestamp(now)
		);

		return new SeedIds(orderId);
	}

	private static Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private record SeedIds(UUID orderId) {
	}
}
