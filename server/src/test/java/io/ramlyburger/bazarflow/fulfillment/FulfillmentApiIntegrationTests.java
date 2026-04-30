package io.ramlyburger.bazarflow.fulfillment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ramlyburger.bazarflow.catalog.StorageClass;
import io.ramlyburger.bazarflow.ordering.CreateOrderLineRequest;
import io.ramlyburger.bazarflow.ordering.CreateOrderRequest;
import io.ramlyburger.bazarflow.pricing.CreatePriceBookRequest;
import io.ramlyburger.bazarflow.pricing.CreatePriceRuleRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FulfillmentApiIntegrationTests {

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
	private ObjectMapper objectMapper;

	@Test
	void rejectsUnauthenticatedFulfillmentRequests() throws Exception {
		mockMvc.perform(get("/api/fulfillment/pick-waves"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void createsPickWaveAndDispatchJobForAcceptedOrder() throws Exception {
		LocalDate deliveryDate = LocalDate.now(ZoneOffset.UTC);
		UUID retailerId = createRetailer("Fulfillment Retailer", "FULFILL-REG-001");
		UUID outletId = createOutlet(
				retailerId,
				"Fulfillment Outlet",
				"central",
				LocalTime.of(0, 0),
				LocalTime.of(0, 30)
		);
		UUID skuId = createSku("FULFILL-SKU-001");
		UUID priceBookId = createPriceBook("PB-FULFILL-001");
		createRule(priceBookId, "FULFILL-PRICE-001", skuId, "8.00");
		receiveLot(skuId, "FULFILL-LOT-001", "5.000", deliveryDate.plusDays(30));
		UUID orderId = createAcceptedOrder(retailerId, outletId, skuId, deliveryDate);

		MvcResult pickWaveResult = mockMvc.perform(post("/api/fulfillment/pick-waves")
						.header("X-Correlation-Id", "test-correlation-create-pick-wave")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePickWaveRequest(deliveryDate, "central"))))
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andExpect(jsonPath("$.waveNumber", notNullValue()))
				.andExpect(jsonPath("$.deliveryZone").value("CENTRAL"))
				.andExpect(jsonPath("$.deliveryDate").value(deliveryDate.toString()))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.orderCount").value(1))
				.andExpect(jsonPath("$.lineCount").value(1))
				.andExpect(jsonPath("$.dispatchJobs", hasSize(1)))
				.andExpect(jsonPath("$.dispatchJobs[0].orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.dispatchJobs[0].status").value("PLANNED"))
				.andExpect(jsonPath("$.dispatchJobs[0].slaAtRisk").value(true))
				.andExpect(jsonPath("$.dispatchJobs[0].slaRiskReason").value("DELIVERY_WINDOW_AT_RISK"))
				.andReturn();

		JsonNode pickWave = objectMapper.readTree(pickWaveResult.getResponse().getContentAsString());
		String pickWaveId = pickWave.get("id").asText();

		mockMvc.perform(get("/api/fulfillment/pick-waves/{pickWaveId}", pickWaveId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pickWaveId))
				.andExpect(jsonPath("$.dispatchJobs", hasSize(1)));

		mockMvc.perform(get("/api/fulfillment/sla-risk"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
				.andExpect(jsonPath("$[0].slaRiskReason").value("DELIVERY_WINDOW_AT_RISK"));

		mockMvc.perform(get("/api/audit/events")
						.param("aggregateType", "ORDER")
						.param("aggregateId", orderId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[4].eventType").value("DISPATCH_PLANNED"))
				.andExpect(jsonPath("$[4].sourceModule").value("fulfillment"));

		mockMvc.perform(post("/api/fulfillment/pick-waves")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePickWaveRequest(deliveryDate, "central"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("NO_ACCEPTED_ORDERS_READY"));
	}

	private UUID createAcceptedOrder(UUID retailerId, UUID outletId, UUID skuId, LocalDate deliveryDate) throws Exception {
		UUID orderId = createDraftOrder(retailerId, outletId, skuId, deliveryDate)
				.andExpect(status().isCreated())
				.andReturnOrderId();

		mockMvc.perform(post("/api/orders/{orderId}/submit", orderId)
						.header("Idempotency-Key", "fulfillment-submit-" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"));

		mockMvc.perform(post("/api/orders/{orderId}/accept", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));

		return orderId;
	}

	private OrderResultActions createDraftOrder(UUID retailerId, UUID outletId, UUID skuId, LocalDate deliveryDate) throws Exception {
		return new OrderResultActions(
				mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOrderRequest(
								retailerId,
								outletId,
								deliveryDate,
								List.of(new CreateOrderLineRequest(skuId, new BigDecimal("1.000")))
						))))
		);
	}

	private UUID createPriceBook(String code) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/pricing/price-books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceBookRequest(
								code,
								code + " Book",
								"MYR",
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private void createRule(UUID priceBookId, String ruleCode, UUID skuId, String unitPrice) throws Exception {
		mockMvc.perform(post("/api/pricing/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceRuleRequest(
								priceBookId,
								ruleCode,
								"Fulfillment test price",
								skuId,
								null,
								null,
								BigDecimal.ZERO,
								new BigDecimal(unitPrice),
								1,
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isCreated());
	}

	private void receiveLot(UUID skuId, String lotCode, String quantity, LocalDate expiryDate) throws Exception {
		mockMvc.perform(post("/api/inventory/lots")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"skuId", skuId,
								"lotCode", lotCode,
								"warehouseCode", "MAIN-WAREHOUSE",
								"receivedQuantity", new BigDecimal(quantity),
								"expiryDate", expiryDate
						)))
				)
				.andExpect(status().isCreated());
	}

	private UUID createRetailer(String legalName, String registrationNumber) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/retailers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"legalName", legalName,
								"tradingName", legalName,
								"registrationNumber", registrationNumber
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private UUID createOutlet(
			UUID retailerId,
			String name,
			String deliveryZone,
			LocalTime deliveryWindowStart,
			LocalTime deliveryWindowEnd
	) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/retailers/{retailerId}/outlets", retailerId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", name,
								"deliveryZone", deliveryZone,
								"addressLine1", "1 Test Road",
								"city", "Test City",
								"state", "Test State",
								"postalCode", "10000",
								"deliveryWindowStart", deliveryWindowStart.toString(),
								"deliveryWindowEnd", deliveryWindowEnd.toString()
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private UUID createSku(String skuCode) throws Exception {
		UUID productId = createProduct(skuCode + " Product");

		MvcResult result = mockMvc.perform(post("/api/skus")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"productId", productId,
								"skuCode", skuCode,
								"name", skuCode + " Item",
								"unitOfMeasure", "pack",
								"storageClass", StorageClass.FROZEN,
								"shelfLifeDays", 180
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private UUID createProduct(String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", name,
								"category", "Fulfillment Test",
								"description", "Fulfillment test product"
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private class OrderResultActions {

		private final org.springframework.test.web.servlet.ResultActions actions;

		OrderResultActions(org.springframework.test.web.servlet.ResultActions actions) {
			this.actions = actions;
		}

		OrderResultActions andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
			actions.andExpect(matcher);
			return this;
		}

		UUID andReturnOrderId() throws Exception {
			MvcResult result = actions.andReturn();
			JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
			return UUID.fromString(response.get("id").asText());
		}
	}
}
