package io.ramlyburger.bazarflow.partner;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
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
class PartnerApiIntegrationTests {

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
	void rejectsUnauthenticatedPartnerRequests() throws Exception {
		mockMvc.perform(get("/api/retailers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void createsRetailerAndOutlet() throws Exception {
		UUID retailerId = createRetailer("TST-1001", "Arctic Fresh Trading");

		mockMvc.perform(get("/api/retailers/{retailerId}", retailerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.legalName").value("Arctic Fresh Trading"))
				.andExpect(jsonPath("$.creditStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.outlets", hasSize(0)));

		mockMvc.perform(post("/api/retailers/{retailerId}/outlets", retailerId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOutletRequest(
								"Northside Store",
								"NORTH-02",
								"12 Cold Chain Avenue",
								"Northport",
								"Central State",
								"10000",
								LocalTime.of(9, 0),
								LocalTime.of(12, 0)
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.deliveryZone").value("NORTH-02"))
				.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(get("/api/retailers/{retailerId}", retailerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outlets", hasSize(1)))
				.andExpect(jsonPath("$.outlets[0].name").value("Northside Store"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsDuplicateRetailerRegistrationNumber() throws Exception {
		createRetailer("TST-1002", "Central Frozen Supply");

		mockMvc.perform(post("/api/retailers")
						.header("X-Correlation-Id", "test-correlation-duplicate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateRetailerRequest(
								"Central Frozen Supply Duplicate",
								null,
								"tst-1002"
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(header().string("X-Correlation-Id", "test-correlation-duplicate"))
				.andExpect(jsonPath("$.errorCode").value("RETAILER_REGISTRATION_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-duplicate"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void returnsProblemDetailsForValidationErrors() throws Exception {
		mockMvc.perform(post("/api/retailers")
						.header("X-Correlation-Id", "test-correlation-validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("legalName", "")))
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-validation"))
				.andExpect(jsonPath("$.errors[0].field").value("legalName"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void rejectsInvalidDeliveryWindow() throws Exception {
		UUID retailerId = createRetailer("TST-1004", "Westline Frozen Supply");

		mockMvc.perform(post("/api/retailers/{retailerId}/outlets", retailerId)
						.header("X-Correlation-Id", "test-correlation-window")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOutletRequest(
								"Evening Store",
								"METRO-01",
								"18 Industrial Road",
								"Metro City",
								"Central State",
								"10001",
								LocalTime.of(14, 0),
								LocalTime.of(14, 0)
						)))
				)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("INVALID_DELIVERY_WINDOW"))
				.andExpect(jsonPath("$.correlationId").value("test-correlation-window"));
	}

	@Test
	@WithMockUser(roles = "OPS_MANAGER")
	void updatesCreditStatus() throws Exception {
		UUID retailerId = createRetailer("TST-1003", "Northline Frozen Supply");

		mockMvc.perform(patch("/api/retailers/{retailerId}/credit-status", retailerId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new UpdateCreditStatusRequest(CreditStatus.BLOCKED))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.creditStatus").value("BLOCKED"));
	}

	private UUID createRetailer(String registrationNumber, String legalName) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/retailers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateRetailerRequest(
								legalName,
								null,
								registrationNumber
						)))
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(response.get("id").asText());
	}
}
