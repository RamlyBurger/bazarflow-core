package io.ramlyburger.bazarflow.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final String resourceClientId;

	public SecurityConfig(@Value("${bazarflow.security.resource-client-id:bazarflow-api}") String resourceClientId) {
		this.resourceClientId = resourceClientId;
	}

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/actuator/health/**",
								"/actuator/info",
								"/api/platform/status",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				);

		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
		return converter;
	}

	@Bean
	KeycloakJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
		return new KeycloakJwtGrantedAuthoritiesConverter(resourceClientId);
	}
}
