package com.my.craft.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Binds {@code keycloak.admin.*} -- credentials for a separate admin-REST-API connection to
 * Keycloak, distinct from the end-user OIDC login / JWT resource-server config in {@code
 * SecurityConfig}. {@code adminRealm} is the realm the client authenticates AGAINST (Keycloak's
 * built-in {@code master} realm holds the {@code KC_BOOTSTRAP_ADMIN_USERNAME}/{@code _PASSWORD}
 * account from infras/local/compose.yml); {@code realm} is the realm users/roles are managed IN
 * -- this app's own realm, matching the OIDC {@code issuer-uri}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    private String serverUrl = "http://localhost:8080";
    private String adminRealm = "master";
    private String realm = "bootstrap";
    private String clientId = "admin-cli";
    private String username = "admin";
    private String password = "admin";
}
