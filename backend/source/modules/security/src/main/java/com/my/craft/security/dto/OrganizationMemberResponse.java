package com.my.craft.security.dto;

import com.my.craft.security.domain.UserOrganizationRole;

/** API-facing shape for a {@link UserOrganizationRole} row. */
public record OrganizationMemberResponse(String userId, String userName, Long roleId, String roleName) {

    public static OrganizationMemberResponse from(UserOrganizationRole membership) {
        return new OrganizationMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getRole().getId(),
                membership.getRole().getName());
    }
}
