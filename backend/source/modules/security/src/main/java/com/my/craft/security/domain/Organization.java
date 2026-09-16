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

/** An organization -- table {@code organizations}. */
@Entity
@Table(name = "organizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization {

    /** The one org {@code MasterAccountInitializer} guarantees exists, reserved outside the
     * normal {@code IDENTITY} sequence (which starts at 1) so it never collides with one a real
     * caller creates through {@code OrganizationController}. */
    public static final long MASTER_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    public Organization(String name) {
        this.name = name;
    }
}
