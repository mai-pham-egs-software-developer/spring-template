package com.my.craft.security.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "This {@link User} holds this {@link Role} within this {@link Organization}" -- table {@code
 * user_organization_roles}, the join entity for the ternary many-to-many a plain
 * {@code @ManyToMany}/{@code @JoinTable} can't express (that only relates two entities; this
 * relates three, with its own FK to each). A given {@code (user, organization)} pair can hold
 * several roles (several rows sharing those two FKs), and a role can apply to several
 * {@code (user, organization)} pairs -- the {@code unique} constraint below just stops the exact
 * same triple being inserted twice, not either of those.
 *
 * <p>{@code role.getOrganization()} is expected to equal {@code organization} here (a role is
 * only meaningful within the org that owns it) -- not enforced at the DB level, so check it
 * before persisting one of these.
 */
@Entity
@Table(
        name = "user_organization_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id", "role_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOrganizationRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public UserOrganizationRole(User user, Organization organization, Role role) {
        this.user = user;
        this.organization = organization;
        this.role = role;
    }
}
