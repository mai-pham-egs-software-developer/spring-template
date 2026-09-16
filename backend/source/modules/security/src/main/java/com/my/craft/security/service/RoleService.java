package com.my.craft.security.service;

import java.util.List;

import com.my.craft.security.dto.RoleRequest;
import com.my.craft.security.dto.RoleResponse;

/** CRUD over {@code Role}, always scoped to the {@code Organization} that owns it. */
public interface RoleService {

    /** @throws OrganizationNotFoundException if {@code organizationId} doesn't exist. */
    List<RoleResponse> findByOrganization(Long organizationId);

    /** @throws RoleNotFoundException if {@code roleId} doesn't exist, or exists under a
     * different organization. */
    RoleResponse findById(Long organizationId, Long roleId);

    /** @throws OrganizationNotFoundException if {@code organizationId} doesn't exist. */
    RoleResponse create(Long organizationId, RoleRequest request);

    /** @throws RoleNotFoundException if {@code roleId} doesn't exist, or exists under a
     * different organization. */
    RoleResponse update(Long organizationId, Long roleId, RoleRequest request);

    /** @throws RoleNotFoundException if {@code roleId} doesn't exist, or exists under a
     * different organization. */
    void delete(Long organizationId, Long roleId);
}
