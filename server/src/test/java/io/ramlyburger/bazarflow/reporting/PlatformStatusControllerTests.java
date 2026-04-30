package io.ramlyburger.bazarflow.reporting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ramlyburger.bazarflow.identity.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlatformStatusController.class)
@Import(SecurityConfig.class)
class PlatformStatusControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesBootstrapPlatformStatus() throws Exception {
		mockMvc.perform(get("/api/platform/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.service").value("bazarflow-core"))
				.andExpect(jsonPath("$.phase").value("active-development"))
				.andExpect(jsonPath("$.modules[0].name").value("partner"))
				.andExpect(jsonPath("$.modules[0].status").value("implemented"));
	}
}
