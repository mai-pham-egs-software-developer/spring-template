package com.my.craft.security.service.initial;

import java.util.List;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.security.config.MasterAccountProperties;
import com.my.craft.security.domain.Organization;
import com.my.craft.security.domain.Permission;
import com.my.craft.security.domain.Role;
import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserOrganizationRole;
import com.my.craft.security.repository.OrganizationJpaRepository;
import com.my.craft.security.repository.PermissionJpaRepository;
import com.my.craft.security.repository.RoleJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;
import com.my.craft.security.repository.UserOrganizationRoleJpaRepository;
import com.my.craft.security.service.UserService;

/**
 * Ensures the {@code app.master-account.*} account exists in the identity provider on every
 * boot -- see {@link UserService#initMasterAccount()} -- then mirrors it locally: a {@link User}
 * row for it, the reserved {@link Organization#MASTER_ID} org, a {@code SUPER_ADMIN} {@link
 * Role} scoped to that org holding a single wildcard {@link Permission} ({@code
 * resource=actionType="*"}), and a {@link UserOrganizationRole} linking all three -- **and**,
 * since {@code CasbinAuthorizationManager} reads {@code casbin_rule} directly, not these JPA
 * tables, the actual {@code g}/{@code p} rows that give the master account teeth: {@code g,
 * <userId>, SUPER_ADMIN, 0} (it holds that role in the master org) and {@code p, SUPER_ADMIN, 0,
 * *, *, *} (that role can do anything, to anything, within the master org -- see
 * rbac_model.conf). Every step is idempotent (find-or-create), so re-running this on an
 * already-bootstrapped DB is a no-op.
 *
 * <p>Left to fail startup on error, the same fail-fast stance {@code
 * CasbinModelConfigInitializer} takes for its own one-time bootstrap work, rather than silently
 * leaving a fresh environment with no way to log in as admin.
 */
@Component
public class MasterAccountInitializer implements ApplicationRunner {

    private static final String MASTER_ORG_NAME = "Master Organization";
    private static final String SUPER_ADMIN_ROLE_NAME = "SUPER_ADMIN";
    private static final String WILDCARD_PERMISSION_NAME = "ALL";
    private static final String WILDCARD = "*";
    private static final String MASTER_ORG_ID = String.valueOf(Organization.MASTER_ID);

    private final UserService userService;
    private final MasterAccountProperties masterAccountProperties;
    private final UserJpaRepository userRepository;
    private final OrganizationJpaRepository organizationRepository;
    private final RoleJpaRepository roleRepository;
    private final PermissionJpaRepository permissionRepository;
    private final UserOrganizationRoleJpaRepository userOrganizationRoleRepository;
    private final Enforcer enforcer;

    public MasterAccountInitializer(
            UserService userService,
            MasterAccountProperties masterAccountProperties,
            UserJpaRepository userRepository,
            OrganizationJpaRepository organizationRepository,
            RoleJpaRepository roleRepository,
            PermissionJpaRepository permissionRepository,
            UserOrganizationRoleJpaRepository userOrganizationRoleRepository,
            Enforcer enforcer) {
        this.userService = userService;
        this.masterAccountProperties = masterAccountProperties;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userOrganizationRoleRepository = userOrganizationRoleRepository;
        this.enforcer = enforcer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String userId = userService.initMasterAccount();
        if (userId == null) {
            return;
        }

        organizationRepository.insertWithExplicitId(Organization.MASTER_ID, MASTER_ORG_NAME);
        Organization masterOrg = organizationRepository
                .findById(Organization.MASTER_ID)
                .orElseThrow(() -> new IllegalStateException("Master organization missing right after insert"));

        User user = userRepository.findById(userId).orElseGet(() -> userRepository.save(new User(userId, masterAccountProperties.getUsername())));

        Role superAdminRole = roleRepository
                .findByOrganizationIdAndName(Organization.MASTER_ID, SUPER_ADMIN_ROLE_NAME)
                .orElseGet(() -> roleRepository.save(new Role(SUPER_ADMIN_ROLE_NAME, masterOrg)));

        Permission wildcardPermission = permissionRepository
                .findByResourceAndActionType(WILDCARD, WILDCARD)
                .orElseGet(() -> permissionRepository.save(new Permission(WILDCARD_PERMISSION_NAME, WILDCARD, WILDCARD)));

        if (!superAdminRole.getPermissions().contains(wildcardPermission)) {
            superAdminRole.addPermission(wildcardPermission);
            roleRepository.save(superAdminRole);
        }

        boolean alreadyAssigned = userOrganizationRoleRepository.existsByUserIdAndOrganizationIdAndRoleId(
                user.getId(), Organization.MASTER_ID, superAdminRole.getId());
        if (!alreadyAssigned) {
            userOrganizationRoleRepository.save(new UserOrganizationRole(user, masterOrg, superAdminRole));
        }

        ensureCasbinGrant(userId);
    }

    /** {@code Role}/{@code Permission}/{@code UserOrganizationRole} above are plain JPA rows --
     * {@code CasbinAuthorizationManager} never reads them. This is what actually makes the
     * master account able to call any RBAC-protected path: a {@code g} row tying it to {@code
     * SUPER_ADMIN} within the master org, and a wildcard {@code p} row for that role/org. */
    private void ensureCasbinGrant(String userId) {
        List<String> roleAssignment = List.of(userId, SUPER_ADMIN_ROLE_NAME, MASTER_ORG_ID);
        if (!enforcer.hasGroupingPolicy(roleAssignment)) {
            enforcer.addGroupingPolicy(roleAssignment);
        }

        List<String> allowAll = List.of(SUPER_ADMIN_ROLE_NAME, MASTER_ORG_ID, WILDCARD, WILDCARD, WILDCARD);
        if (!enforcer.hasPolicy(allowAll)) {
            enforcer.addPolicy(allowAll);
        }
    }
}
