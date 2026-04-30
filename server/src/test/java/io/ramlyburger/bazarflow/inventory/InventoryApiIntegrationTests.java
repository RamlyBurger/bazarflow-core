package io.ramlyburger.bazarflow.inventory;

import static org.hamcrest.Matchers.hasItem;
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
class InventoryApiIntegrationTests {

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
	void rejectsUnauthenticatedInventoryRequests() throws Exception {
		mockMvc.perform(get("/api/inventory/lots"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void receivesLotAndReportsAvailability() throws Exception {
		UUID skuId = createSku("INV-FROZEN-001");

		mockMvc.perform(post("/api/inventory/lots")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateInventoryLotRequest(
								skuId,
								"lot-a1",
								"main-warehouse",
								new BigDecimal("25.500"),
								LocalDate.now(ZoneOffset.UTC).plusDays(90)
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andExpect(jsonPath("$.skuId").value(skuId.toString()))
				.andExpect(jsonPath("$.lotCode").value("LOT-A1"))
				.andExpect(jsonPath("$.warehouseCode").value("MAIN-WAREHOUSE"))
				.andExpect(jsonPath("$.availableQuantity").value(25.5))
				.andExpect(jsonPath("$.reservedQuantity").value(0))
				.andExpect(jsonPath("$.status").value("AVAILABLE"));

		mockMvc.perform(get("/api/inventory/availability").param("skuId", skuId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skuId").value(skuId.toString()))
				.andExpect(jsonPath("$.availableQuantity").value(25.5))
				.andExpect(jsonPath("$.reservedQuantity").value(0))
				.andExpect(jsonPath("$.availableLotCount").value(1));

		mockMvc.perform(get("/api/inventory/lots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].lotCode", hasItem("LOT-A1")));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDuplicateLotCodeForSku() throws Exception {
		UUID skuId = createSku("INV-DUP-001");
		receiveLot(skuId, "LOT-DUP");

		mockMvc.perform(post("/api/inventory/lots")
						.header("X-Correlation-Id", "test-correlation-lot-duplicate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateInventoryLotRequest(
								skuId,
								"lot-dup",
								"main-warehouse",
								new BigDecimal("5.000"),
								LocalDate.now(ZoneOffset.UTC).plusDays(45)
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("INVENTORY_LOT_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-lot-duplicate"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsLotForMissingSku() throws Exception {
		mockMvc.perform(post("/api/inventory/lots")
						.header("X-Correlation-Id", "test-correlation-missing-sku")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateInventoryLotRequest(
								UUID.randomUUID(),
								"LOT-MISSING",
								"main-warehouse",
								new BigDecimal("5.000"),
								LocalDate.now(ZoneOffset.UTC).plusDays(30)
						)))
				)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-missing-sku"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsPastExpiryDate() throws Exception {
		UUID skuId = createSku("INV-EXPIRY-001");

		mockMvc.perform(post("/api/inventory/lots")
						.header("X-Correlation-Id", "test-correlation-expiry")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateInventoryLotRequest(
								skuId,
								"LOT-EXPIRED",
								"main-warehouse",
								new BigDecimal("5.000"),
								LocalDate.now(ZoneOffset.UTC).minusDays(1)
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("INVALID_EXPIRY_DATE"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-expiry"));
	}

	private void receiveLot(UUID skuId, String lotCode) throws Exception {
		mockMvc.perform(post("/api/inventory/lots")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateInventoryLotRequest(
								skuId,
								lotCode,
								"MAIN-WAREHOUSE",
								new BigDecimal("10.000"),
								LocalDate.now(ZoneOffset.UTC).plusDays(60)
						)))
				)
				.andExpect(status().isCreated());
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
								"category", "Inventory Test",
								"description", "Inventory test product"
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}
}
