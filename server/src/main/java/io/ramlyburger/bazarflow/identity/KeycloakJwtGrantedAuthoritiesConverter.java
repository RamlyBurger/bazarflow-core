package io.ramlyburger.bazarflow.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
	private final String clientId;

	KeycloakJwtGrantedAuthoritiesConverter(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
		Set<GrantedAuthority> authorities = new LinkedHashSet<>(
				scopeAuthorities == null ? Collections.emptyList() : scopeAuthorities
		);
		addRoles(authorities, jwt.getClaimAsMap("realm_access"));

		Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess != null) {
			Object clientAccess = resourceAccess.get(clientId);
			if (clientAccess instanceof Map<?, ?> clientAccessClaims) {
				addRoles(authorities, clientAccessClaims);
			}
		}

		return authorities;
	}

	private static void addRoles(Set<GrantedAuthority> authorities, Map<?, ?> accessClaims) {
		if (accessClaims == null) {
			return;
		}

		Object rolesClaim = accessClaims.get("roles");
		if (!(rolesClaim instanceof Collection<?> roles)) {
			return;
		}

		for (Object role : roles) {
			if (role instanceof String roleName && !roleName.isBlank()) {
				authorities.add(new SimpleGrantedAuthority(toRoleAuthority(roleName)));
			}
		}
	}

	private static String toRoleAuthority(String roleName) {
		String normalized = roleName.trim().replace('-', '_').toUpperCase(Locale.ROOT);
		if (normalized.startsWith(ROLE_PREFIX)) {
			return normalized;
		}
		return ROLE_PREFIX + normalized;
	}
}
