package com.my.craft.security.domain.operator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The live Casbin RBAC model definition (the {@code sub, obj, act} / matcher text otherwise
 * bundled as the classpath {@code rbac_model.conf}) -- table {@code casbin_model_config}, a
 * single row keyed by {@link #DEFAULT_ID}. Separate from {@code casbin_rule}: that table is owned
 * entirely by casbin-spring-boot-starter's JDBC adapter (policy data), this one is a plain
 * Hibernate-managed entity (schema, editable via {@code CasbinPolicyController}'s
 * {@code /config} endpoint) that {@code CasbinModelConfigInitializer} applies to the
 * already-built {@code Enforcer} at startup via {@code setModel}.
 */
@Entity
@Table(name = "casbin_model_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CasbinModelConfig {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public CasbinModelConfig(String id, String content) {
        this.id = id;
        this.content = content;
    }
}
