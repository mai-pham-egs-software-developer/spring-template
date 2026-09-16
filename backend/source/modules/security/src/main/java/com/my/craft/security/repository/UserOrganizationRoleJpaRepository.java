package com.my.craft.security.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.UserOrganizationRole;

public interface UserOrganizationRoleJpaRepository extends JpaRepository<UserOrganizationRole, Long> {

    List<UserOrganizationRole> findByUserId(String userId);

    List<UserOrganizationRole> findByUserIdAndOrganizationId(String userId, Long organizationId);

    List<UserOrganizationRole> findByOrganizationId(Long organizationId);

    boolean existsByUserIdAndOrganizationIdAndRoleId(String userId, Long organizationId, Long roleId);
}
