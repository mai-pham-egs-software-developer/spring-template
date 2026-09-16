package com.my.craft.security.config;

import com.my.craft.security.service.initial.MasterAccountInitializer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Binds {@code app.master-account.*} -- the one account {@link
 * MasterAccountInitializer} guarantees exists in the identity
 * provider on every boot (created if missing, password reset, {@code realmRole} assigned),
 * whether that provider is Keycloak ({@link com.my.craft.security.service.KeycloakUserService})
 * or something else entirely (Cognito, ...) behind the same {@code UserService} port.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.master-account")
public class MasterAccountProperties {

    private boolean enabled = true;
    private String username = "master";
    private String email = "master@example.com";

    // CHANGE-ME: dev-only default so a fresh checkout has a working admin login out of the box --
    // set a real value (env var/secret) before any shared/non-local deployment.
    private String password = "ChangeMe123!";

    private String realmRole = "admin";
}
