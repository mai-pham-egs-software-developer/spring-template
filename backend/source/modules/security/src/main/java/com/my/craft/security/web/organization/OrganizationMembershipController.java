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
import com.my.craft.security.dto.OrganizationRequest;
import com.my.craft.security.dto.OrganizationResponse;
import com.my.craft.security.security.UserContextUtils;
import com.my.craft.security.service.OrganizationMembershipService;
import com.my.craft.security.service.OrganizationService;

/**
 * Plain CRUD over {@code Organization} -- delegates to {@link OrganizationService}. See {@link
 * OrganizationUserController} for the user-in-org / org-of-user relationship, and {@code
 * RoleController} for role CRUD within one org.
 */
@RestController
@RequestMapping(Constants.BASE_PATH + "/organizations")
@RequiresPermission(resource = "organization", action = "READ")
@AllArgsConstructor
public class OrganizationMembershipController {

    private final OrganizationMembershipService membershipService;


    // No @RequiresPermission here -- this only ever returns the caller's own orgs (userId comes
    // from the authenticated token, not a path variable), so there's nothing to gate. Mapped
    // ahead of "/{id}" below by Spring's usual literal-beats-variable path matching. Listed in
    // casbin.rbac-exempt-paths so CasbinAuthorizationManager grants it outright.
    @GetMapping("/me")
    public List<OrganizationResponse> listMine() {
        String userId = UserContextUtils.currentUserContext().userId();
        return membershipService.findOrganizationsByUser(userId);
    }

}
