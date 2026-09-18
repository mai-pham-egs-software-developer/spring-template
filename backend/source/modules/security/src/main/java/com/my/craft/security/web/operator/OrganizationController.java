package com.my.craft.security.web.operator;

import com.my.craft.security.annotation.RequiresPermission;
import com.my.craft.security.dto.AddOrganizationMemberRequest;
import com.my.craft.security.dto.OrganizationMemberResponse;
import com.my.craft.security.dto.OrganizationRequest;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.dto.RoleRequest;
import com.my.craft.security.dto.RoleResponse;
import com.my.craft.security.dto.UpdateOrganizationMemberRequest;
import com.my.craft.security.service.OrganizationMembershipService;
import com.my.craft.security.service.OrganizationService;
import com.my.craft.security.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plain CRUD over {@code Organization} -- delegates to {@link OrganizationService} -- plus its
 * two owned sub-resources: {@code Role} (via {@link RoleService}) and organization membership
 * (via {@link OrganizationMembershipService}). See {@link com.my.craft.security.web.operator.UserController#organizationsOf}
 * for the reverse lookup (every org a given user belongs to).
 */
@RestController
@RequestMapping(Constants.BASE_PATH + "/organizations")
@RequiresPermission(resource = "organization", action = "READ")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final RoleService roleService;
    private final OrganizationMembershipService membershipService;

    public OrganizationController(
            OrganizationService organizationService, RoleService roleService, OrganizationMembershipService membershipService) {
        this.organizationService = organizationService;
        this.roleService = roleService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<OrganizationResponse> list() {
        return organizationService.findAll();
    }

    @RequiresPermission(resource = "organization", action = "READ", idParam = "id")
    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable Long id) {
        return organizationService.findById(id);
    }

    @RequiresPermission(resource = "organization", action = "WRITE")
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @RequiresPermission(resource = "organization", action = "WRITE", idParam = "id")
    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable Long id, @RequestBody OrganizationRequest request) {
        return organizationService.update(id, request);
    }

    @RequiresPermission(resource = "organization", action = "DELETE", idParam = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Roles -- always scoped to the organization named in the path. ----

    @RequiresPermission(resource = "role", action = "READ", orgIdParam = "organizationId")
    @GetMapping("/{organizationId}/roles")
    public List<RoleResponse> listRoles(@PathVariable Long organizationId) {
        return roleService.findByOrganization(organizationId);
    }

    @RequiresPermission(resource = "role", action = "READ", idParam = "roleId", orgIdParam = "organizationId")
    @GetMapping("/{organizationId}/roles/{roleId}")
    public RoleResponse getRole(@PathVariable Long organizationId, @PathVariable Long roleId) {
        return roleService.findById(organizationId, roleId);
    }

    @RequiresPermission(resource = "role", action = "WRITE", orgIdParam = "organizationId")
    @PostMapping("/{organizationId}/roles")
    public ResponseEntity<RoleResponse> createRole(@PathVariable Long organizationId, @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(organizationId, request));
    }

    @RequiresPermission(resource = "role", action = "WRITE", idParam = "roleId", orgIdParam = "organizationId")
    @PutMapping("/{organizationId}/roles/{roleId}")
    public RoleResponse updateRole(@PathVariable Long organizationId, @PathVariable Long roleId, @RequestBody RoleRequest request) {
        return roleService.update(organizationId, roleId, request);
    }

    @RequiresPermission(resource = "role", action = "DELETE", idParam = "roleId", orgIdParam = "organizationId")
    @DeleteMapping("/{organizationId}/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long organizationId, @PathVariable Long roleId) {
        roleService.delete(organizationId, roleId);
        return ResponseEntity.noContent().build();
    }

    // ---- Members -- assign/unassign a user to the organization named in the path, by role. A
    // (organizationId, userId) pair holds at most one role through this API -- see
    // OrganizationMembershipService. ----

    @RequiresPermission(resource = "organization-member", action = "READ", orgIdParam = "organizationId")
    @GetMapping("/{organizationId}/users")
    public List<OrganizationMemberResponse> listMembers(@PathVariable Long organizationId) {
        return membershipService.findMembers(organizationId);
    }

    @RequiresPermission(resource = "organization-member", action = "WRITE", orgIdParam = "organizationId")
    @PostMapping("/{organizationId}/users")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable Long organizationId, @RequestBody AddOrganizationMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipService.addMember(organizationId, request));
    }

    @RequiresPermission(resource = "organization-member", action = "WRITE", idParam = "userId", orgIdParam = "organizationId")
    @PutMapping("/{organizationId}/users/{userId}")
    public OrganizationMemberResponse updateMember(
            @PathVariable Long organizationId, @PathVariable String userId, @RequestBody UpdateOrganizationMemberRequest request) {
        return membershipService.updateMember(organizationId, userId, request);
    }

    @RequiresPermission(resource = "organization-member", action = "DELETE", idParam = "userId", orgIdParam = "organizationId")
    @DeleteMapping("/{organizationId}/users/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long organizationId, @PathVariable String userId) {
        membershipService.removeMember(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}
