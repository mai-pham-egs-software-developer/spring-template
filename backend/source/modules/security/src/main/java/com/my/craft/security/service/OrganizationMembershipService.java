package com.my.craft.security.service;

import java.util.List;

import com.my.craft.security.dto.AddOrganizationMemberRequest;
import com.my.craft.security.dto.OrganizationMemberResponse;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.dto.UpdateOrganizationMemberRequest;

/**
 * Manages {@code UserOrganizationRole} rows -- "this user holds this role within this org" --
 * for one {@code Organization} at a time. Treats a {@code (organizationId, userId)} pair as
 * holding at most one role through this API: {@link #addMember}/{@link #updateMember} replace
 * whatever row(s) already exist for that pair rather than adding another one alongside it. The
 * underlying table itself doesn't enforce that (a user can genuinely hold several roles in the
 * same org), so a caller that needs that goes straight to {@code UserOrganizationRoleJpaRepository}
 * instead of this service.
 */
public interface OrganizationMembershipService {

    /** @throws OrganizationNotFoundException if {@code organizationId} doesn't exist. */
    List<OrganizationMemberResponse> findMembers(Long organizationId);

    /**
     * @throws OrganizationNotFoundException if {@code organizationId} doesn't exist.
     * @throws UserNotFoundException if {@code request.userId()} doesn't exist.
     * @throws RoleNotFoundException if {@code request.roleId()} doesn't exist, or exists under a
     *     different organization.
     */
    OrganizationMemberResponse addMember(Long organizationId, AddOrganizationMemberRequest request);

    /**
     * @throws OrganizationMemberNotFoundException if {@code userId} isn't currently a member.
     * @throws RoleNotFoundException if {@code request.roleId()} doesn't exist, or exists under a
     *     different organization.
     */
    OrganizationMemberResponse updateMember(Long organizationId, String userId, UpdateOrganizationMemberRequest request);

    /** @throws OrganizationMemberNotFoundException if {@code userId} isn't currently a member. */
    void removeMember(Long organizationId, String userId);

    /** Every org the given user belongs to, regardless of role. @throws UserNotFoundException if
     * {@code userId} doesn't exist. */
    List<OrganizationResponse> findOrganizationsByUser(String userId);
}
