package com.my.craft.security.domain;

import com.my.craft.security.domain.operator.AdminRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A persisted user profile -- table {@code users}. {@code id} is the Keycloak subject claim
 * ({@code sub}) rather than a generated key, since the identity already lives in Keycloak and
 * this row only needs to be a stable local FK target for role assignment. Deliberately separate
 * from {@link com.my.craft.security.security.UserContext}: that's a per-request view built from
 * whatever authenticated the current call, this is the durable row one role is assigned to.
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

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private AdminRole role;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
