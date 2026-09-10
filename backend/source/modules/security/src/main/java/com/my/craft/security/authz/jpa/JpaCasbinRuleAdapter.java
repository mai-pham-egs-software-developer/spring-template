package com.my.craft.security.authz.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Helper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * jcasbin {@link Adapter} backed by {@link CasbinRuleJpaRepository} -- makes RBAC policy live in
 * the {@code casbin_rule} table instead of the bundled rbac_policy.csv, so it can be changed at
 * runtime without a redeploy. Wired into the {@code Enforcer} bean in {@code CasbinConfig}.
 *
 * <p>{@link #loadPolicy(Model)} only runs once, when the {@code Enforcer} bean is built (jcasbin
 * doesn't poll the database); {@link #addPolicy}/{@link #removePolicy} are what keep a running
 * instance's in-memory copy and the DB in sync afterwards -- they fire automatically whenever
 * policy is changed through {@code Enforcer}'s management API (e.g. {@code
 * enforcer.addPolicy(...)}, see {@code CasbinPolicyController}), which is the supported way to
 * change policy at runtime. A change made by writing to {@code casbin_rule} directly (outside the
 * app, e.g. via psql) is invisible to an already-running {@code Enforcer} until something calls
 * {@link org.casbin.jcasbin.main.Enforcer#loadPolicy()} again -- see docs/security.md.
 */
@Component
public class JpaCasbinRuleAdapter implements Adapter {

    private static final String[] FIELDS = {"v0", "v1", "v2", "v3", "v4", "v5"};

    private final CasbinRuleJpaRepository repository;

    public JpaCasbinRuleAdapter(CasbinRuleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void loadPolicy(Model model) {
        for (CasbinRuleEntity entity : repository.findAll()) {
            Helper.loadPolicyLine(toLine(entity), model);
        }
    }

    @Override
    @Transactional
    public void savePolicy(Model model) {
        repository.deleteAllInBatch();
        List<CasbinRuleEntity> rows = new ArrayList<>();
        rows.addAll(collectRules(model, "p"));
        rows.addAll(collectRules(model, "g"));
        repository.saveAll(rows);
    }

    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        repository.save(toEntity(ptype, rule));
    }

    @Override
    @Transactional
    public void removePolicy(String sec, String ptype, List<String> rule) {
        repository
                .findByPtypeAndV0AndV1AndV2AndV3AndV4AndV5(
                        ptype, at(rule, 0), at(rule, 1), at(rule, 2), at(rule, 3), at(rule, 4), at(rule, 5))
                .forEach(repository::delete);
    }

    @Override
    @Transactional
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        Specification<CasbinRuleEntity> spec = (root, query, cb) -> cb.equal(root.get("ptype"), ptype);
        for (int i = 0; i < fieldValues.length; i++) {
            String value = fieldValues[i];
            int column = fieldIndex + i;
            if (value != null && !value.isEmpty() && column < FIELDS.length) {
                String field = FIELDS[column];
                spec = spec.and((root, query, cb) -> cb.equal(root.get(field), value));
            }
        }
        repository.deleteAll(repository.findAll(spec));
    }

    private static List<CasbinRuleEntity> collectRules(Model model, String section) {
        List<CasbinRuleEntity> rows = new ArrayList<>();
        Map<String, Assertion> assertions = model.model.get(section);
        if (assertions == null) {
            return rows;
        }
        assertions.forEach((ptype, assertion) -> assertion.policy.forEach(rule -> rows.add(toEntity(ptype, rule))));
        return rows;
    }

    private static CasbinRuleEntity toEntity(String ptype, List<String> rule) {
        return new CasbinRuleEntity(ptype, at(rule, 0), at(rule, 1), at(rule, 2), at(rule, 3), at(rule, 4), at(rule, 5));
    }

    private static String toLine(CasbinRuleEntity entity) {
        List<String> values = new ArrayList<>();
        for (String value : new String[] {entity.getV0(), entity.getV1(), entity.getV2(), entity.getV3(), entity.getV4(), entity.getV5()}) {
            if (value == null) {
                break;
            }
            values.add(value);
        }
        return entity.getPtype() + ", " + String.join(", ", values);
    }

    private static String at(List<String> rule, int index) {
        return index < rule.size() ? rule.get(index) : null;
    }
}
