package com.my.craft.security.service.initial;

import java.util.List;

import com.my.craft.security.service.OrganizationNotFoundException;
import com.my.craft.security.service.RoleNotFoundException;
import com.my.craft.security.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.auditlog.service.AuditActionRecorder;
import com.my.craft.security.dto.RoleRequest;
import com.my.craft.security.dto.RoleResponse;
import com.my.craft.security.domain.Organization;
import com.my.craft.security.domain.Role;
import com.my.craft.security.repository.OrganizationJpaRepository;
import com.my.craft.security.repository.RoleJpaRepository;

/**
 * {@code Role.organization} is {@code FetchType.LAZY} and {@code open-in-view} is off, so every
 * method that reads it (directly, via {@code RoleResponse.from}, or via {@code
 * AuditActionRecorder.record}/{@code recordDeletion} snapshotting the entity) needs to stay
 * inside a transaction for the whole read+map -- {@code @Transactional} here does that.
 */
@Service
public class DefaultRoleService implements RoleService {

    private final OrganizationJpaRepository organizationRepository;
    private final RoleJpaRepository roleRepository;
    private final AuditActionRecorder auditActionRecorder;

    public DefaultRoleService(
            OrganizationJpaRepository organizationRepository, RoleJpaRepository roleRepository, AuditActionRecorder auditActionRecorder) {
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.auditActionRecorder = auditActionRecorder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findByOrganization(Long organizationId) {
        requireOrganization(organizationId);
        return roleRepository.findByOrganizationId(organizationId).stream().map(RoleResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long organizationId, Long roleId) {
        return RoleResponse.from(requireRole(organizationId, roleId));
    }

    @Override
    @Transactional
    public RoleResponse create(Long organizationId, RoleRequest request) {
        Organization organization = requireOrganization(organizationId);
        Role saved = roleRepository.save(new Role(request.name(), organization));
        auditActionRecorder.record("ROLE_CREATED", saved);
        return RoleResponse.from(saved);
    }

    @Override
    @Transactional
    public RoleResponse update(Long organizationId, Long roleId, RoleRequest request) {
        Role role = requireRole(organizationId, roleId);
        role.setName(request.name());
        Role saved = roleRepository.save(role);
        auditActionRecorder.record("ROLE_UPDATED", saved);
        return RoleResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long organizationId, Long roleId) {
        Role role = requireRole(organizationId, roleId);
        roleRepository.deleteById(roleId);
        auditActionRecorder.recordDeletion("ROLE_DELETED", role);
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId).orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private Role requireRole(Long organizationId, Long roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        if (!role.getOrganization().getId().equals(organizationId)) {
            throw new RoleNotFoundException(roleId);
        }
        return role;
    }
}
