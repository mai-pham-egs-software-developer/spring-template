package com.my.craft.security.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A persisted user profile -- table {@code users}. {@code id} is an app-generated, stable local
 * key -- deliberately NOT the Keycloak subject claim, because this row is written (status {@code
 * PENDING}) before the identity provider has even been called: {@code
 * DefaultUserAdminService.create} inserts it and an {@link OutboxEvent} together in one local
 * transaction and returns immediately, without ever calling Keycloak itself. {@code
 * com.my.craft.security.service.outbox.OutboxWorker} does that call asynchronously and, on
 * success, calls {@link #activate} to fill {@link #keycloakId} and flip {@link #status} to {@code
 * ACTIVE}. This is the outbox pattern for the Keycloak/DB dual write -- see
 * {@code backend/docs/user-outbox.md} for the full design.
 *
 * <p>Deliberately separate from {@link com.my.craft.security.security.UserContext}: that's a
 * per-request view built straight from the Keycloak subject claim on whatever JWT authenticated
 * the current call (independent of this table entirely), this is the durable row. Casbin RBAC
 * (see {@code com.my.craft.security.authz.CasbinAuthorizationManager}) is keyed by that same JWT
 * subject claim too, never by {@link #id} -- so decoupling this id from Keycloak's doesn't affect
 * how permissions are enforced, only how an admin would reference a *specific* user resource
 * (path {@code /operators/users/{id}}) when hand-writing a fine-grained policy row.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, updatable = false)
    private String username;

    @Column(nullable = false, updatable = false)
    private String email;

    @Setter
    @Column(nullable = false)
    private String name;

    /** Null while {@link #status} is {@code PENDING} -- filled in by {@link #activate}. */
    @Column(name = "keycloak_id")
    private String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public User(String username, String email, String name) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.name = name;
        this.status = UserStatus.PENDING;
    }

    /** Called once {@code OutboxWorker} successfully syncs this row to the identity provider --
     * either it created a brand-new account, or adopted an already-existing one confidently
     * judged to be the same person (see {@code CreateUserOutcome}). */
    public void activate(String keycloakId) {
        this.keycloakId = keycloakId;
        this.status = UserStatus.ACTIVE;
    }

    /** The identity provider has an account for this username that isn't confidently the same
     * person -- needs a human, not an automatic retry. */
    public void markConflict() {
        this.status = UserStatus.CONFLICT;
    }

    /** The identity provider permanently rejected creating this account. */
    public void markFailed() {
        this.status = UserStatus.FAILED;
    }
}
