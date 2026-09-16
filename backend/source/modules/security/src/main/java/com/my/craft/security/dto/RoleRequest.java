package com.my.craft.security.dto;

/** Organization comes from the path ({@code /organizations/{organizationId}/roles}), not the body. */
public record RoleRequest(String name) {}
