package io.ramlyburger.bazarflow.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtGrantedAuthoritiesConverterTests {

	private final KeycloakJwtGrantedAuthoritiesConverter converter =
			new KeycloakJwtGrantedAuthoritiesConverter("bazarflow-api");

	@Test
	void mapsRealmAndClientRolesToSpringAuthorities() {
		Jwt jwt = jwt(Map.of(
				"realm_access", Map.of("roles", List.of("OPS_MANAGER", "auditor")),
				"resource_access", Map.of(
						"bazarflow-api", Map.of("roles", List.of("warehouse", "ROLE_DISPATCH")),
						"other-client", Map.of("roles", List.of("ignored"))
				),
				"scope", "openid profile"
		));

		assertThat(converter.convert(jwt))
				.extracting(GrantedAuthority::getAuthority)
				.contains(
						"ROLE_OPS_MANAGER",
						"ROLE_AUDITOR",
						"ROLE_WAREHOUSE",
						"ROLE_DISPATCH",
						"SCOPE_openid",
						"SCOPE_profile"
				)
				.doesNotContain("ROLE_IGNORED");
	}

	@Test
	void returnsScopeAuthoritiesWhenKeycloakRoleClaimsAreMissing() {
		Jwt jwt = jwt(Map.of("scope", "orders.read"));

		assertThat(converter.convert(jwt))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("SCOPE_orders.read");
	}

	private static Jwt jwt(Map<String, Object> claims) {
		return new Jwt(
				"token",
				Instant.now(),
				Instant.now().plusSeconds(600),
				Map.of("alg", "none"),
				claims
		);
	}
}
