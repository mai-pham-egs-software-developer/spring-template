package com.my.craft.security.authz.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CasbinRuleJpaRepository extends JpaRepository<CasbinRuleEntity, Long>, JpaSpecificationExecutor<CasbinRuleEntity> {

    List<CasbinRuleEntity> findByPtypeAndV0AndV1AndV2AndV3AndV4AndV5(
            String ptype, String v0, String v1, String v2, String v3, String v4, String v5);
}
