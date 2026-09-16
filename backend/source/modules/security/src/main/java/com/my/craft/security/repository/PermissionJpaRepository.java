package com.my.craft.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.Permission;

public interface PermissionJpaRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndActionType(String resource, String actionType);
}
