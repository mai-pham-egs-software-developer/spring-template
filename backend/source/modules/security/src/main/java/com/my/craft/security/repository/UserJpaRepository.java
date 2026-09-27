package com.my.craft.security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserStatus;

public interface UserJpaRepository extends JpaRepository<User, String> {

    Optional<User> findByKeycloakId(String keycloakId);

    List<User> findByStatus(UserStatus status);

    long countByStatus(UserStatus status);
}
