package com.my.craft.security.authz.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One jcasbin policy or role-assignment row -- table {@code casbin_rule}, the schema every
 * official Casbin adapter (Go, Node, the JDBC adapter, ...) uses, so any Casbin-aware admin tool
 * can read/write this table directly. {@code ptype} is {@code "p"} for a policy line (matching
 * {@code p = sub, obj, act} in rbac_model.conf, so v0/v1/v2 hold sub/obj/act) or {@code "g"} for a
 * role assignment ({@code g, <username>, <role>}, so v0/v1 hold username/role); v3-v5 stay unused
 * by this model but exist for parity with the standard schema. See JpaCasbinRuleAdapter /
 * docs/security.md.
 */
@Entity
@Table(name = "casbin_rule")
public class CasbinRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String ptype;

    @Column(name = "v0")
    private String v0;

    @Column(name = "v1")
    private String v1;

    @Column(name = "v2")
    private String v2;

    @Column(name = "v3")
    private String v3;

    @Column(name = "v4")
    private String v4;

    @Column(name = "v5")
    private String v5;

    /** JPA. */
    protected CasbinRuleEntity() {}

    public CasbinRuleEntity(String ptype, String v0, String v1, String v2, String v3, String v4, String v5) {
        this.ptype = ptype;
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
    }

    public Long getId() {
        return id;
    }

    public String getPtype() {
        return ptype;
    }

    public String getV0() {
        return v0;
    }

    public String getV1() {
        return v1;
    }

    public String getV2() {
        return v2;
    }

    public String getV3() {
        return v3;
    }

    public String getV4() {
        return v4;
    }

    public String getV5() {
        return v5;
    }
}
