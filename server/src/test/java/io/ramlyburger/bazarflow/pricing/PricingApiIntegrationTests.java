package io.ramlyburger.bazarflow.pricing;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class PricingApiIntegrationTests {

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
	void rejectsUnauthenticatedPricingRequests() throws Exception {
		mockMvc.perform(get("/api/pricing/rules"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void createsPriceBookRuleAndQuotesBestTier() throws Exception {
		UUID retailerId = createRetailer("Pricing Tier Retailer", "PRICE-TIER-001");
		UUID skuId = createSku("PRICE-TIER-SKU-001");
		UUID priceBookId = createPriceBook("PB-PRICE-TIER", "Pricing Tier Book");

		createRule(priceBookId, "BASE-PRICE-TIER", skuId, null, null, "12.50", "0.000", 10);
		createRule(priceBookId, "CONTRACT-PRICE-TIER", skuId, retailerId, null, "10.75", "10.000", 20);

		mockMvc.perform(post("/api/pricing/quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceQuoteRequest(
								retailerId,
								"north",
								java.util.List.of(new PriceQuoteItemRequest(skuId, new BigDecimal("12.000")))
						)))
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retailerId").value(retailerId.toString()))
				.andExpect(jsonPath("$.deliveryZone").value("NORTH"))
				.andExpect(jsonPath("$.currency").value("MYR"))
				.andExpect(jsonPath("$.subtotal").value(129.00))
				.andExpect(jsonPath("$.appliedRuleCount").value(1))
				.andExpect(jsonPath("$.lines[0].unitPrice").value(10.75))
				.andExpect(jsonPath("$.lines[0].currency").value("MYR"))
				.andExpect(jsonPath("$.lines[0].lineTotal").value(129.00))
				.andExpect(jsonPath("$.lines[0].appliedRules[0].ruleCode").value("CONTRACT-PRICE-TIER"));

		mockMvc.perform(get("/api/pricing/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDuplicateRuleCode() throws Exception {
		UUID skuId = createSku("PRICE-DUP-SKU-001");
		UUID priceBookId = createPriceBook("PB-PRICE-DUP", "Pricing Duplicate Book");
		createRule(priceBookId, "DUPLICATE-RULE", skuId, null, null, "8.00", "0.000", 1);

		mockMvc.perform(post("/api/pricing/rules")
						.header("X-Correlation-Id", "test-correlation-price-duplicate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceRuleRequest(
								priceBookId,
								"duplicate-rule",
								"Duplicate test rule",
								skuId,
								null,
								null,
								BigDecimal.ZERO,
								new BigDecimal("8.50"),
								1,
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PRICE_RULE_CODE_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-price-duplicate"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsRuleForMissingSku() throws Exception {
		UUID priceBookId = createPriceBook("PB-PRICE-MISSING-SKU", "Pricing Missing SKU Book");

		mockMvc.perform(post("/api/pricing/rules")
						.header("X-Correlation-Id", "test-correlation-price-missing-sku")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceRuleRequest(
								priceBookId,
								"MISSING-SKU-RULE",
								"Missing SKU rule",
								UUID.randomUUID(),
								null,
								null,
								BigDecimal.ZERO,
								new BigDecimal("7.25"),
								1,
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-price-missing-sku"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsQuoteWithoutMatchingRule() throws Exception {
		UUID retailerId = createRetailer("Pricing Missing Rule Retailer", "PRICE-MISSING-RULE-001");
		UUID skuId = createSku("PRICE-MISSING-RULE-SKU-001");

		mockMvc.perform(post("/api/pricing/quote")
						.header("X-Correlation-Id", "test-correlation-no-price-rule")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceQuoteRequest(
								retailerId,
								"south",
								java.util.List.of(new PriceQuoteItemRequest(skuId, new BigDecimal("1.000")))
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PRICE_RULE_NOT_FOUND"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-no-price-rule"));
	}

	private UUID createPriceBook(String code, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/pricing/price-books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceBookRequest(
								code,
								name,
								"myr",
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private UUID createRule(
			UUID priceBookId,
			String ruleCode,
			UUID skuId,
			UUID retailerId,
			String deliveryZone,
			String unitPrice,
			String minQuantity,
			int priority
	) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/pricing/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreatePriceRuleRequest(
								priceBookId,
								ruleCode,
								"Pricing test rule",
								skuId,
								retailerId,
								deliveryZone,
								new BigDecimal(minQuantity),
								new BigDecimal(unitPrice),
								priority,
								LocalDate.now(ZoneOffset.UTC).minusDays(1),
								null
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
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

	private UUID createSku(String skuCode) throws Exception {
		UUID productId = createProduct(skuCode + " Product");

		MvcResult result = mockMvc.perform(post("/api/skus")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"productId", productId,
								"skuCode", skuCode,
								"name", skuCode + " Item",
								"unitOfMeasure", "pack",
								"storageClass", "FROZEN",
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
								"category", "Pricing Test",
								"description", "Pricing test product"
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}
}
