package com.my.craft.security.web.organization;

import java.util.List;

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
import com.my.craft.security.dto.RoleRequest;
import com.my.craft.security.dto.RoleResponse;
import com.my.craft.security.service.RoleService;

/** CRUD over {@code Role}, always scoped to its owning {@code Organization} via the path --
 * delegates to {@link RoleService}. Every method repeats {@code orgIdParam = "organizationId"}
 * since {@code CasbinAuthorizationManager} only reads one {@code RequiresPermission} (method,
 * else class) per request, never merges the two. */
@RestController
@RequestMapping(Constants.BASE_PATH + "/organizations/{organizationId}/roles")
@RequiresPermission(resource = "role", action = "READ", orgIdParam = "organizationId")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @RequiresPermission(resource = "role", action = "READ", orgIdParam = "organizationId")
    @GetMapping
    public List<RoleResponse> list(@PathVariable Long organizationId) {
        return roleService.findByOrganization(organizationId);
    }

    @RequiresPermission(resource = "role", action = "READ", idParam = "roleId", orgIdParam = "organizationId")
    @GetMapping("/{roleId}")
    public RoleResponse get(@PathVariable Long organizationId, @PathVariable Long roleId) {
        return roleService.findById(organizationId, roleId);
    }

    @RequiresPermission(resource = "role", action = "WRITE", orgIdParam = "organizationId")
    @PostMapping
    public ResponseEntity<RoleResponse> create(@PathVariable Long organizationId, @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(organizationId, request));
    }

    @RequiresPermission(resource = "role", action = "WRITE", idParam = "roleId", orgIdParam = "organizationId")
    @PutMapping("/{roleId}")
    public RoleResponse update(@PathVariable Long organizationId, @PathVariable Long roleId, @RequestBody RoleRequest request) {
        return roleService.update(organizationId, roleId, request);
    }

    @RequiresPermission(resource = "role", action = "DELETE", idParam = "roleId", orgIdParam = "organizationId")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable Long organizationId, @PathVariable Long roleId) {
        roleService.delete(organizationId, roleId);
        return ResponseEntity.noContent().build();
    }
}
