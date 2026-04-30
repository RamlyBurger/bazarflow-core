package io.ramlyburger.bazarflow.identity;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiSecurityConfig {

	private static final String BEARER_AUTH = "bearerAuth";

	@Bean
	OpenAPI bazarflowOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("BazarFlow Core API")
						.version("0.0.1")
						.description("Operations API for orders, inventory, fulfillment, audit, and reporting"))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Keycloak-issued JWT bearer token")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
