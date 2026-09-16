package com.my.craft.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One grantable permission -- table {@code permissions}. {@code resource} is the thing the
 * permission applies to (e.g. an API resource or feature area); {@code actionType} is what may be
 * done to it (e.g. {@code READ}/{@code WRITE}/{@code DELETE}); {@link Role} holds the
 * many-to-many to this via the {@code role_permission} join table.
 */
@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private String resource;

    @Setter
    @Column(nullable = false)
    private String actionType;

    public Permission(String name, String resource, String actionType) {
        this.name = name;
        this.resource = resource;
        this.actionType = actionType;
    }
}
