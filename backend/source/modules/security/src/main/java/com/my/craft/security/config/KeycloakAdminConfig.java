package com.my.craft.security.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KeycloakAdminProperties.class, MasterAccountProperties.class, OutboxProperties.class})
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getAdminRealm())
                .clientId(properties.getClientId())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }
}
