package com.my.craft.security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserStatus;

/** No blanket {@code @JaversSpringDataAuditable} here -- audit entries for {@link User} are
 * written explicitly, only for marked actions, via {@code AuditActionRecorder} in {@code
 * DefaultUserAdminService} -- see backend/docs/audit-log.md. */
public interface UserJpaRepository extends JpaRepository<User, String> {

    Optional<User> findByKeycloakId(String keycloakId);

    List<User> findByStatus(UserStatus status);

    long countByStatus(UserStatus status);
}
