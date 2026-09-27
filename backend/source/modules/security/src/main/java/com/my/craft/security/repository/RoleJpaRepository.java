package com.my.craft.security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.Role;

public interface RoleJpaRepository extends JpaRepository<Role, Long> {

    List<Role> findByOrganizationId(Long organizationId);

    Optional<Role> findByOrganizationIdAndName(Long organizationId, String name);
}
