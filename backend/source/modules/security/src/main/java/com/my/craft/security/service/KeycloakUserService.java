package com.my.craft.security.service;

import java.util.List;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.my.craft.security.config.KeycloakAdminProperties;
import com.my.craft.security.config.MasterAccountProperties;

import jakarta.ws.rs.core.Response;

@Service
public class KeycloakUserService implements UserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);

    private final Keycloak keycloakAdminClient;
    private final KeycloakAdminProperties adminProperties;
    private final MasterAccountProperties masterAccountProperties;

    public KeycloakUserService(
            Keycloak keycloakAdminClient, KeycloakAdminProperties adminProperties, MasterAccountProperties masterAccountProperties) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.adminProperties = adminProperties;
        this.masterAccountProperties = masterAccountProperties;
    }

    @Override
    public void createUser() {}

    @Override
    public void updateUser() {}

    @Override
    public void deactivateUser() {}

    @Override
    public String initMasterAccount() {
        if (!masterAccountProperties.isEnabled()) {
            log.info("Master account bootstrap disabled (app.master-account.enabled=false)");
            return null;
        }

        RealmResource realm = keycloakAdminClient.realm(adminProperties.getRealm());
        UsersResource users = realm.users();
        String username = masterAccountProperties.getUsername();

        String userId = users.search(username, true).stream()
                .findFirst()
                .map(UserRepresentation::getId)
                .orElseGet(() -> createMasterUser(users));

        resetPassword(users, userId);
        assignRealmRole(realm, users, userId);
        log.info("Master account '{}' ready in realm '{}'", username, adminProperties.getRealm());
        return userId;
    }

    private String createMasterUser(UsersResource users) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(masterAccountProperties.getUsername());
        user.setEmail(masterAccountProperties.getEmail());
        user.setEnabled(true);
        user.setEmailVerified(true);

        try (Response response = users.create(user)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException(
                        "Failed to create master account '%s' in Keycloak: HTTP %d".formatted(user.getUsername(), response.getStatus()));
            }
            String id = CreatedResponseUtil.getCreatedId(response);
            log.info("Created master account '{}' ({}) in realm '{}'", user.getUsername(), id, adminProperties.getRealm());
            return id;
        }
    }

    private void resetPassword(UsersResource users, String userId) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(masterAccountProperties.getPassword());
        credential.setTemporary(false);
        users.get(userId).resetPassword(credential);
    }

    private void assignRealmRole(RealmResource realm, UsersResource users, String userId) {
        String roleName = masterAccountProperties.getRealmRole();
        RoleRepresentation role = findOrCreateRealmRole(realm, roleName);

        boolean alreadyAssigned =
                users.get(userId).roles().realmLevel().listAll().stream().anyMatch(r -> r.getName().equals(roleName));
        if (!alreadyAssigned) {
            users.get(userId).roles().realmLevel().add(List.of(role));
        }
    }

    private RoleRepresentation findOrCreateRealmRole(RealmResource realm, String roleName) {
        return realm.roles().list().stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElseGet(() -> {
                    RoleRepresentation newRole = new RoleRepresentation();
                    newRole.setName(roleName);
                    realm.roles().create(newRole);
                    return realm.roles().get(roleName).toRepresentation();
                });
    }
}
