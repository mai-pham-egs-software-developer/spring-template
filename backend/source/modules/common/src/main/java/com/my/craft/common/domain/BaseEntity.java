package com.my.craft.common.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * Base for a JPA entity with a DB-generated surrogate key -- {@code Long id}, {@code
 * GenerationType.IDENTITY} -- matching every entity in this codebase that doesn't have a specific
 * reason to do otherwise (e.g. {@code com.my.craft.security.domain.Organization}/{@code
 * Role}/{@code Permission}/{@code UserOrganizationRole}/{@code OutboxEvent}).
 *
 * <p>An entity whose id has to come from somewhere else -- e.g. {@code
 * com.my.craft.security.domain.User}'s app-generated UUID, assigned in its own constructor before
 * the identity provider is ever called (see {@code backend/docs/user-outbox.md}) -- should
 * <strong>not</strong> extend this: {@code @GeneratedValue} can't be overridden or turned off for
 * a field a subclass merely inherits, so a different id shape means declaring {@code @Id}
 * directly on that entity instead, the way {@code User} already does.
 *
 * <p>Equality and hashing are by id alone (the standard JPA-entity-equality approach), falling
 * back to reference identity while {@code id} is still null -- i.e. before the entity is first
 * persisted -- so two not-yet-saved instances (or an instance compared against itself before and
 * after its first save) are never accidentally treated as equal or unequal by surprise.
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other) || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
