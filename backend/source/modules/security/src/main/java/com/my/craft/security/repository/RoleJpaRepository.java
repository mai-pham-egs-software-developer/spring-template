package com.my.craft.security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.Role;

/** No blanket {@code @JaversSpringDataAuditable} here -- audit entries for {@link Role} are
 * written explicitly, only for marked actions, via {@code AuditActionRecorder} in {@code
 * DefaultRoleService} -- see backend/docs/audit-log.md. */
public interface RoleJpaRepository extends JpaRepository<Role, Long> {

    List<Role> findByOrganizationId(Long organizationId);

    Optional<Role> findByOrganizationIdAndName(Long organizationId, String name);
}
