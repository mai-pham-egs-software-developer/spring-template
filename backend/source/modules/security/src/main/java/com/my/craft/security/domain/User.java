package com.my.craft.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A persisted user profile -- table {@code users}. {@code id} is the Keycloak subject claim
 * ({@code sub}) rather than a generated key, since the identity already lives in Keycloak and
 * this row only needs to be a stable local FK target for role assignment (see {@link
 * UserOrganizationRole}). Deliberately separate from {@link
 * com.my.craft.security.security.UserContext}: that's a per-request view built from whatever
 * authenticated the current call, this is the durable row.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Setter
    @Column(nullable = false)
    private String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
