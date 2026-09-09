package com.my.craft.security.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.my.craft.security.user.UserContextEnrichmentFilter;
import com.my.craft.security.user.UserService;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    // Binds the top-level `security:` YAML sequence directly onto a List bean
    // (no wrapper object needed) -- see docs/security.md.
    @Bean
    @ConfigurationProperties(prefix = "security")
    public List<SecurityRuleProperties> securityRules() {
        return new ArrayList<>();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain basicAuthSecurityFilterChain(
            HttpSecurity http, List<SecurityRuleProperties> securityRules, UserService userService) throws Exception {
        List<String> paths = pathsOf(securityRules, AuthType.BASIC);
        if (paths.isEmpty()) {
            return null;
        }
        http
                .securityMatcher(paths.toArray(String[]::new))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(basicUserDetailsService(securityRules))
                .addFilterAfter(new UserContextEnrichmentFilter(userService), AuthorizationFilter.class);
        return http.build();
    }

    // All auth-type: BEARER rules share one chain, scoped to just their
    // paths, validated as Bearer JWTs against the configured issuer.
    @Bean
    @Order(2)
    public SecurityFilterChain bearerAuthSecurityFilterChain(
            HttpSecurity http, List<SecurityRuleProperties> securityRules, UserService userService) throws Exception {
        List<String> paths = pathsOf(securityRules, AuthType.BEARER);
        if (paths.isEmpty()) {
            return null;
        }
        http
                .securityMatcher(paths.toArray(String[]::new))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(new UserContextEnrichmentFilter(userService), AuthorizationFilter.class);
        return http.build();
    }

    // Everything not covered by an explicit `security:` rule (e.g. /me) keeps
    // the original behaviour: session-based OIDC login for browser navigation,
    // Bearer JWT resource server for API calls, both on the same endpoints.
    // This is also what makes CORS work for those paths -- .cors(...) only
    // takes effect on requests that match SOME SecurityFilterChain; without
    // this catch-all, a path matching none of them gets no CORS headers at
    // all and the browser blocks it, even though corsConfigurationSource()
    // is configured correctly.
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, UserService userService) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(new UserContextEnrichmentFilter(userService), AuthorizationFilter.class);
        return http.build();
    }

    // Explicit seam for pulling the raw token out of the request -- default
    // behaviour is the standard "Authorization: Bearer <token>" header. Swap
    // this bean for a custom BearerTokenResolver (cookie, query param, ...)
    // if a client can't set the Authorization header itself.
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }

    // Turns a validated Jwt into the Authentication Spring Security/@PreAuthorize
    // sees: keeps the default "scope"/"scp" -> SCOPE_* authorities and adds
    // Keycloak's realm-level roles (the non-standard "realm_access.roles"
    // claim) as ROLE_* authorities, so hasRole(...)/hasAuthority(...) work
    // against Keycloak roles the same way they would for any other setup.
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new HashSet<>(scopeAuthoritiesConverter.convert(jwt));
            authorities.addAll(realmRoleAuthorities(jwt));
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> realmRoleAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Collection<String> roles = realmAccess == null
                ? List.of()
                : (Collection<String>) realmAccess.getOrDefault("roles", List.of());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }

    private static List<String> pathsOf(List<SecurityRuleProperties> rules, AuthType authType) {
        return rules.stream()
                .filter(rule -> rule.getAuthType() == authType)
                .map(SecurityRuleProperties::getPath)
                .toList();
    }

    private static InMemoryUserDetailsManager basicUserDetailsService(List<SecurityRuleProperties> rules) {
        List<UserDetails> users = rules.stream()
                .filter(rule -> rule.getAuthType() == AuthType.BASIC)
                .map(rule -> User.withUsername(rule.getUsername())
                        .password("{noop}" + rule.getPassword())
                        .roles("USER")
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
