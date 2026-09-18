package com.my.craft.security.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.auditlog.service.AuditActionRecorder;
import com.my.craft.security.dto.AddOrganizationMemberRequest;
import com.my.craft.security.dto.OrganizationMemberResponse;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.dto.UpdateOrganizationMemberRequest;
import com.my.craft.security.domain.Organization;
import com.my.craft.security.domain.Role;
import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserOrganizationRole;
import com.my.craft.security.repository.OrganizationJpaRepository;
import com.my.craft.security.repository.RoleJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;
import com.my.craft.security.repository.UserOrganizationRoleJpaRepository;

@Service
public class DefaultOrganizationMembershipService implements OrganizationMembershipService {

    private final OrganizationJpaRepository organizationRepository;
    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final UserOrganizationRoleJpaRepository membershipRepository;
    private final AuditActionRecorder auditActionRecorder;

    public DefaultOrganizationMembershipService(
            OrganizationJpaRepository organizationRepository,
            UserJpaRepository userRepository,
            RoleJpaRepository roleRepository,
            UserOrganizationRoleJpaRepository membershipRepository,
            AuditActionRecorder auditActionRecorder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.auditActionRecorder = auditActionRecorder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> findMembers(Long organizationId) {
        requireOrganization(organizationId);
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(OrganizationMemberResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public OrganizationMemberResponse addMember(Long organizationId, AddOrganizationMemberRequest request) {
        Organization organization = requireOrganization(organizationId);
        User user = requireUser(request.userId());
        Role role = requireRole(organizationId, request.roleId());
        replaceMembership(organizationId, user.getId());
        UserOrganizationRole saved = membershipRepository.save(new UserOrganizationRole(user, organization, role));
        auditActionRecorder.record("ORGANIZATION_MEMBER_ADDED", saved);
        return OrganizationMemberResponse.from(saved);
    }

    @Override
    @Transactional
    public OrganizationMemberResponse updateMember(Long organizationId, String userId, UpdateOrganizationMemberRequest request) {
        List<UserOrganizationRole> existing = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId);
        if (existing.isEmpty()) {
            throw new OrganizationMemberNotFoundException(organizationId, userId);
        }
        User user = existing.get(0).getUser();
        Organization organization = existing.get(0).getOrganization();
        Role role = requireRole(organizationId, request.roleId());
        membershipRepository.deleteAll(existing);
        UserOrganizationRole saved = membershipRepository.save(new UserOrganizationRole(user, organization, role));
        auditActionRecorder.record("ORGANIZATION_MEMBER_UPDATED", saved);
        return OrganizationMemberResponse.from(saved);
    }

    @Override
    @Transactional
    public void removeMember(Long organizationId, String userId) {
        List<UserOrganizationRole> existing = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId);
        if (existing.isEmpty()) {
            throw new OrganizationMemberNotFoundException(organizationId, userId);
        }
        membershipRepository.deleteAll(existing);
        existing.forEach(membership -> auditActionRecorder.recordDeletion("ORGANIZATION_MEMBER_REMOVED", membership));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> findOrganizationsByUser(String userId) {
        requireUser(userId);
        return membershipRepository.findByUserId(userId).stream()
                .map(UserOrganizationRole::getOrganization)
                .distinct()
                .map(OrganizationResponse::from)
                .toList();
    }

    private void replaceMembership(Long organizationId, String userId) {
        List<UserOrganizationRole> existing = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId);
        if (!existing.isEmpty()) {
            membershipRepository.deleteAll(existing);
        }
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId).orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Role requireRole(Long organizationId, Long roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        if (!role.getOrganization().getId().equals(organizationId)) {
            throw new RoleNotFoundException(roleId);
        }
        return role;
    }
}
