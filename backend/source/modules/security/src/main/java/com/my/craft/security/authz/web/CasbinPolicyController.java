package com.my.craft.security.authz.web;

import java.util.List;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runtime RBAC management: every call here goes through {@link Enforcer}'s management API, which
 * both updates the in-memory policy this instance enforces immediately AND persists the change
 * via {@code JpaCasbinRuleAdapter} (table {@code casbin_rule}) -- the supported way to change
 * policy without a redeploy. See docs/security.md.
 *
 * <p>This controller's own path ({@code /api/admin/casbin/*}) is itself BEARER + RBAC protected
 * (see {@code application.yml}'s {@code security:} list and {@code rbac_policy.csv}'s seeded
 * {@code p, admin, /api/admin/casbin/*, *} row) -- only a subject with the {@code admin} role can
 * reach it. In a real app, prefer a dedicated {@code casbin-admin} role over reusing {@code admin}
 * for this.
 */
@RestController
@RequestMapping("/api/admin/casbin")
public class CasbinPolicyController {

    private final Enforcer enforcer;

    public CasbinPolicyController(Enforcer enforcer) {
        this.enforcer = enforcer;
    }

    /** Every {@code p} row: {@code [role, pathPattern, method]}. */
    @GetMapping("/policies")
    public List<List<String>> listPolicies() {
        return enforcer.getPolicy();
    }

    @PostMapping("/policies")
    public void addPolicy(@RequestBody PolicyRule rule) {
        enforcer.addPolicy(rule.role(), rule.pathPattern(), rule.method());
    }

    @DeleteMapping("/policies")
    public void removePolicy(@RequestBody PolicyRule rule) {
        enforcer.removePolicy(rule.role(), rule.pathPattern(), rule.method());
    }

    /** Every {@code g} row: {@code [username, role]}. */
    @GetMapping("/roles")
    public List<List<String>> listRoleAssignments() {
        return enforcer.getGroupingPolicy();
    }

    @PostMapping("/roles")
    public void assignRole(@RequestBody RoleAssignment assignment) {
        enforcer.addRoleForUser(assignment.username(), assignment.role());
    }

    @DeleteMapping("/roles")
    public void unassignRole(@RequestBody RoleAssignment assignment) {
        enforcer.deleteRoleForUser(assignment.username(), assignment.role());
    }

    /**
     * Re-reads every row from {@code casbin_rule}. Only needed to pick up a change made by
     * writing to the table directly (e.g. via psql) instead of through this controller -- calls
     * through {@link Enforcer}'s management API above are already live without it.
     */
    @PostMapping("/reload")
    public void reload() {
        enforcer.loadPolicy();
    }

    public record PolicyRule(String role, String pathPattern, String method) {}

    public record RoleAssignment(String username, String role) {}
}
