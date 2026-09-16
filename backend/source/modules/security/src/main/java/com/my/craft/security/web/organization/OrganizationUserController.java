package com.my.craft.security.web.organization;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.security.annotation.RequiresPermission;
import com.my.craft.security.dto.AddOrganizationMemberRequest;
import com.my.craft.security.dto.OrganizationMemberResponse;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.dto.UpdateOrganizationMemberRequest;
import com.my.craft.security.service.OrganizationMembershipService;

/**
 * The {@code User} <-> {@code Organization} relationship, both directions -- delegates entirely
 * to {@link OrganizationMembershipService}:
 *
 * <ul>
 *   <li>{@code /organizations/{organizationId}/users}: CRUD the users within one selected org
 *       (add/update-role/remove) -- {@code (organizationId, userId)} holds at most one role
 *       through this API, per {@link OrganizationMembershipService}'s own contract.
 *   <li>{@code /users/{userId}/organizations}: the reverse lookup -- every org a given user
 *       belongs to. Listed in {@code casbin.rbac-exempt-paths}, so it's open to any
 *       authenticated caller regardless of role/org (no {@link com.my.craft.security.annotation.RequiresPermission} on it).
 * </ul>
 */
@RestController
@RequestMapping(Constants.BASE_PATH)
@AllArgsConstructor
public class OrganizationUserController {

    private final OrganizationMembershipService membershipService;

    @RequiresPermission(resource = "organization-member", action = "READ", orgIdParam = "organizationId")
    @GetMapping("/organizations/{organizationId}/users")
    public List<OrganizationMemberResponse> listUsersInOrganization(@PathVariable Long organizationId) {
        return membershipService.findMembers(organizationId);
    }

    @RequiresPermission(resource = "organization-member", action = "WRITE", orgIdParam = "organizationId")
    @PostMapping("/organizations/{organizationId}/users")
    public ResponseEntity<OrganizationMemberResponse> addUserToOrganization(
            @PathVariable Long organizationId, @RequestBody AddOrganizationMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipService.addMember(organizationId, request));
    }

    @RequiresPermission(resource = "organization-member", action = "WRITE", idParam = "userId", orgIdParam = "organizationId")
    @PutMapping("/organizations/{organizationId}/users/{userId}")
    public OrganizationMemberResponse updateUserInOrganization(
            @PathVariable Long organizationId, @PathVariable String userId, @RequestBody UpdateOrganizationMemberRequest request) {
        return membershipService.updateMember(organizationId, userId, request);
    }

    @RequiresPermission(resource = "organization-member", action = "DELETE", idParam = "userId", orgIdParam = "organizationId")
    @DeleteMapping("/organizations/{organizationId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromOrganization(@PathVariable Long organizationId, @PathVariable String userId) {
        membershipService.removeMember(organizationId, userId);
        return ResponseEntity.noContent().build();
    }

    // No @RequiresPermission here on purpose -- this path is listed in casbin.rbac-exempt-paths,
    // so CasbinAuthorizationManager grants it outright (still requires plain Bearer auth) before
    // ever resolving an annotation. Any callers still need to be authenticated; there's just no
    // per-org role/permission gate on top of that. See docs/security.md.
    @GetMapping("/users/{userId}/organizations")
    public List<OrganizationResponse> listOrganizationsOfUser(@PathVariable String userId) {
        return membershipService.findOrganizationsByUser(userId);
    }
}
