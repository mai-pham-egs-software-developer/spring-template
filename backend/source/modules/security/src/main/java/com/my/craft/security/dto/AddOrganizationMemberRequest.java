package com.my.craft.security.dto;

/** {@code roleId} must name a {@code Role} that belongs to the same organization. */
public record AddOrganizationMemberRequest(String userId, Long roleId) {}
