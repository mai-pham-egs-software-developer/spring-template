package com.my.craft.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.operator.CasbinModelConfig;

public interface CasbinModelConfigJpaRepository extends JpaRepository<CasbinModelConfig, String> {}
