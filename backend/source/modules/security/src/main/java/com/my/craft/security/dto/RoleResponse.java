package com.my.craft.security.dto;

import com.my.craft.security.domain.Role;

/** API-facing shape for {@link Role} -- keeps the JPA entity out of the wire format. */
public record RoleResponse(Long id, String name, Long organizationId) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getOrganization().getId());
    }
}
