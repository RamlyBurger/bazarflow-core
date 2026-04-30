package io.ramlyburger.bazarflow.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CatalogApiIntegrationTests {

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
	void rejectsUnauthenticatedCatalogRequests() throws Exception {
		mockMvc.perform(get("/api/skus"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void createsProductAndSku() throws Exception {
		UUID productId = createProduct("Frozen Dumplings", "Frozen Meals");

		mockMvc.perform(post("/api/skus")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateSkuRequest(
								productId,
								"fd-500g",
								"Frozen Dumplings 500g",
								"pack",
								StorageClass.FROZEN,
								180,
								"9550000000010"
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andExpect(jsonPath("$.productId").value(productId.toString()))
				.andExpect(jsonPath("$.skuCode").value("FD-500G"))
				.andExpect(jsonPath("$.unitOfMeasure").value("PACK"))
				.andExpect(jsonPath("$.storageClass").value("FROZEN"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].metadata.coldChain").value(true));

		mockMvc.perform(get("/api/skus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].productName").value("Frozen Dumplings"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDuplicateSkuCode() throws Exception {
		UUID productId = createProduct("Soup Base", "Pantry");
		createSku(productId, "SOUP-BASE-1KG");

		mockMvc.perform(post("/api/skus")
						.header("X-Correlation-Id", "test-correlation-sku-duplicate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateSkuRequest(
								productId,
								"soup-base-1kg",
								"Soup Base 1kg Duplicate",
								"pack",
								StorageClass.AMBIENT,
								365,
								null
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("SKU_CODE_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-sku-duplicate"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsSkuForMissingProduct() throws Exception {
		mockMvc.perform(post("/api/skus")
						.header("X-Correlation-Id", "test-correlation-missing-product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateSkuRequest(
								UUID.randomUUID(),
								"MISSING-001",
								"Missing Product SKU",
								"pack",
								StorageClass.CHILLED,
								30,
								null
						)))
				)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-missing-product"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void updatesSkuStatus() throws Exception {
		UUID productId = createProduct("Chilled Sauce", "Sauces");
		UUID skuId = createSku(productId, "SAUCE-250ML");

		mockMvc.perform(patch("/api/skus/{skuId}/status", skuId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new UpdateSkuStatusRequest(SkuStatus.DISCONTINUED))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISCONTINUED"));
	}

	private UUID createProduct(String name, String category) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateProductRequest(
								name,
								category,
								"Catalog test product",
								Map.of("coldChain", true)
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}

	private UUID createSku(UUID productId, String skuCode) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/skus")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateSkuRequest(
								productId,
								skuCode,
								skuCode + " Item",
								"pack",
								StorageClass.CHILLED,
								45,
								null
						)))
				)
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}
}
