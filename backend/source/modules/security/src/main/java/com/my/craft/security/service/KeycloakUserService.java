package com.my.craft.security.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

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

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

@Service
public class KeycloakUserService implements UserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);

    // Printable, unambiguous-enough charset for a temporary password shown once and then
    // discarded -- covers upper/lower/digit/symbol so it clears typical Keycloak password policies.
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

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
    public CreateUserOutcome tryCreateUser(String username, String email) {
        UsersResource users = realmUsers();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        user.setEnabled(true);

        try (Response response = users.create(user)) {
            int status = response.getStatus();
            if (status == Response.Status.CREATED.getStatusCode()) {
                String id = CreatedResponseUtil.getCreatedId(response);
                setTemporaryPassword(users, id, generateTemporaryPassword());
                log.info("Created user '{}' ({}) in realm '{}'", username, id, adminProperties.getRealm());
                return new CreateUserOutcome.Created(id);
            }
            if (status == Response.Status.CONFLICT.getStatusCode()) {
                return resolveConflict(users, username, email);
            }
            if (status >= 500) {
                return new CreateUserOutcome.TransientFailure("Keycloak returned HTTP %d creating user '%s'".formatted(status, username));
            }
            return new CreateUserOutcome.PermanentFailure("Keycloak rejected user '%s': HTTP %d".formatted(username, status));
        } catch (ProcessingException e) {
            // Network failure, timeout, connection refused -- Keycloak unreachable right now.
            return new CreateUserOutcome.TransientFailure("Could not reach Keycloak: " + e.getMessage());
        }
    }

    /** Called on HTTP 409 from {@link #tryCreateUser} -- looks the existing account up and
     * decides whether it's confidently the same person (adopt) or not (needs a human). */
    private CreateUserOutcome resolveConflict(UsersResource users, String username, String email) {
        Optional<UserRepresentation> existing = users.search(username, true).stream().findFirst();
        if (existing.isEmpty()) {
            // Keycloak said 409 but a follow-up search finds no match -- drift, not something to
            // retry automatically.
            log.warn("Keycloak returned 409 for '{}' but a follow-up search found no match", username);
            return new CreateUserOutcome.Conflict();
        }
        UserRepresentation found = existing.get();
        if (isSamePerson(found, email)) {
            log.info("Adopting existing Keycloak user '{}' ({}) -- username and email match", username, found.getId());
            return new CreateUserOutcome.Adopted(found.getId());
        }
        log.warn("Keycloak user '{}' ({}) already exists but its email doesn't match -- flagging for manual review", username, found.getId());
        return new CreateUserOutcome.Conflict();
    }

    /** Username already matched (that's how {@code found} was looked up); email is the second
     * signal -- if we don't have one to compare, there's nothing more to check. */
    private static boolean isSamePerson(UserRepresentation found, String expectedEmail) {
        return expectedEmail == null || expectedEmail.isBlank() || expectedEmail.equalsIgnoreCase(found.getEmail());
    }

    @Override
    public Optional<String> findKeycloakIdByUsername(String username) {
        return realmUsers().search(username, true).stream().findFirst().map(UserRepresentation::getId);
    }

    private void setTemporaryPassword(UsersResource users, String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true);
        users.get(userId).resetPassword(credential);
    }

    private static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

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

    private UsersResource realmUsers() {
        return keycloakAdminClient.realm(adminProperties.getRealm()).users();
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
