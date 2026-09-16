package com.my.craft.security.web.operator;

import com.my.craft.security.annotation.RequiresPermission;
import com.my.craft.security.dto.OrganizationRequest;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.security.UserContextUtils;
import com.my.craft.security.service.OrganizationMembershipService;
import com.my.craft.security.service.OrganizationService;
import com.my.craft.security.web.organization.OrganizationUserController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plain CRUD over {@code Organization} -- delegates to {@link OrganizationService}. See {@link
 * OrganizationUserController} for the user-in-org / org-of-user relationship, and {@code
 * RoleController} for role CRUD within one org.
 */
@RestController
@RequestMapping(Constants.BASE_PATH + "/organizations")
@RequiresPermission(resource = "organization", action = "READ")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMembershipService membershipService;

    public OrganizationController(OrganizationService organizationService, OrganizationMembershipService membershipService) {
        this.organizationService = organizationService;
        this.membershipService = membershipService;
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
}
