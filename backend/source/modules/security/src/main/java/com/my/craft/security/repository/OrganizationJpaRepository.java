package com.my.craft.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.Organization;

public interface OrganizationJpaRepository extends JpaRepository<Organization, Long> {}
