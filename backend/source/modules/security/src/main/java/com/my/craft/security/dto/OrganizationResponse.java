package com.my.craft.security.dto;

import com.my.craft.security.domain.Organization;

/** API-facing shape for {@link Organization} -- keeps the JPA entity out of the wire format. */
public record OrganizationResponse(Long id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName());
    }
}
