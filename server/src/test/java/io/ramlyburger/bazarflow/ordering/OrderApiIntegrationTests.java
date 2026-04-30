package io.ramlyburger.bazarflow.ordering;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ramlyburger.bazarflow.catalog.StorageClass;
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
class OrderApiIntegrationTests {

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
	void rejectsUnauthenticatedOrderRequests() throws Exception {
		mockMvc.perform(get("/api/orders"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void createsDraftAndSubmitsIdempotently() throws Exception {
		UUID retailerId = createRetailer("Order Retailer", "ORDER-REG-001");
		UUID outletId = createOutlet(retailerId, "Order Outlet", "east");
		UUID skuId = createSku("ORDER-SKU-001");
		UUID priceBookId = createPriceBook("PB-ORDER-001");
		createRule(priceBookId, "ORDER-PRICE-001", skuId, "9.50");

		UUID orderId = createDraftOrder(retailerId, outletId, skuId, "3.000")
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andExpect(jsonPath("$.deliveryZone").value("EAST"))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.currency").value("MYR"))
				.andExpect(jsonPath("$.subtotal").value(28.50))
				.andExpect(jsonPath("$.lines", hasSize(1)))
				.andExpect(jsonPath("$.lines[0].unitPrice").value(9.50))
				.andReturnOrderId();

		mockMvc.perform(post("/api/orders/{orderId}/submit", orderId)
						.header("Idempotency-Key", "submit-order-001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.submittedAt", notNullValue()));

		mockMvc.perform(post("/api/orders/{orderId}/submit", orderId)
						.header("Idempotency-Key", "submit-order-001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"));

		mockMvc.perform(get("/api/orders/{orderId}/timeline", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].toStatus").value("DRAFT"))
				.andExpect(jsonPath("$[1].fromStatus").value("DRAFT"))
				.andExpect(jsonPath("$[1].toStatus").value("SUBMITTED"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsSubmitForBlockedRetailer() throws Exception {
		UUID retailerId = createRetailer("Blocked Order Retailer", "ORDER-BLOCKED-001");
		UUID outletId = createOutlet(retailerId, "Blocked Outlet", "west");
		UUID skuId = createSku("ORDER-BLOCKED-SKU-001");
		UUID priceBookId = createPriceBook("PB-ORDER-BLOCKED");
		createRule(priceBookId, "ORDER-BLOCKED-PRICE", skuId, "6.25");
		UUID orderId = createDraftOrder(retailerId, outletId, skuId, "2.000")
				.andExpect(status().isCreated())
				.andReturnOrderId();

		mockMvc.perform(patch("/api/retailers/{retailerId}/credit-status", retailerId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("creditStatus", "BLOCKED"))))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/orders/{orderId}/submit", orderId)
						.header("X-Correlation-Id", "test-correlation-blocked-submit")
						.header("Idempotency-Key", "blocked-submit-001"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("RETAILER_CREDIT_BLOCKED"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-blocked-submit"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDraftWhenNoPriceRuleMatches() throws Exception {
		UUID retailerId = createRetailer("No Price Retailer", "ORDER-NO-PRICE-001");
		UUID outletId = createOutlet(retailerId, "No Price Outlet", "north");
		UUID skuId = createSku("ORDER-NO-PRICE-SKU-001");

		createDraftOrder(retailerId, outletId, skuId, "1.000")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PRICE_RULE_NOT_FOUND"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDuplicateSkuLines() throws Exception {
		UUID retailerId = createRetailer("Duplicate SKU Retailer", "ORDER-DUP-SKU-001");
		UUID outletId = createOutlet(retailerId, "Duplicate SKU Outlet", "south");
		UUID skuId = createSku("ORDER-DUP-SKU-ITEM-001");
		UUID priceBookId = createPriceBook("PB-ORDER-DUP-SKU");
		createRule(priceBookId, "ORDER-DUP-SKU-PRICE", skuId, "5.50");

		mockMvc.perform(post("/api/orders")
						.header("X-Correlation-Id", "test-correlation-order-duplicate-sku")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOrderRequest(
								retailerId,
								outletId,
								LocalDate.now(ZoneOffset.UTC).plusDays(1),
								List.of(
										new CreateOrderLineRequest(skuId, new BigDecimal("1.000")),
										new CreateOrderLineRequest(skuId, new BigDecimal("2.000"))
								)
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DUPLICATE_ORDER_SKU"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-order-duplicate-sku"));
	}

	private OrderResultActions createDraftOrder(UUID retailerId, UUID outletId, UUID skuId, String quantity) throws Exception {
		return new OrderResultActions(
				mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOrderRequest(
								retailerId,
								outletId,
								LocalDate.now(ZoneOffset.UTC).plusDays(1),
								List.of(new CreateOrderLineRequest(skuId, new BigDecimal(quantity)))
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
								"Order test price",
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

	private UUID createOutlet(UUID retailerId, String name, String deliveryZone) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/retailers/{retailerId}/outlets", retailerId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", name,
								"deliveryZone", deliveryZone,
								"addressLine1", "1 Test Road",
								"city", "Test City",
								"state", "Test State",
								"postalCode", "10000",
								"deliveryWindowStart", LocalTime.of(9, 0).toString(),
								"deliveryWindowEnd", LocalTime.of(17, 0).toString()
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
								"category", "Order Test",
								"description", "Order test product"
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
