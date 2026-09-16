package com.my.craft.security.web.operator;

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
import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;
import com.my.craft.security.service.UserAdminService;

/** Plain CRUD over {@code User} -- delegates to {@link UserAdminService}. */
@RestController
@RequestMapping(Constants.BASE_PATH + "/users")
@RequiresPermission(resource = "user", action = "READ")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userAdminService.findAll();
    }

    @RequiresPermission(resource = "user", action = "READ", idParam = "id")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return userAdminService.findById(id);
    }

    @RequiresPermission(resource = "user", action = "WRITE")
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.create(request));
    }

    @RequiresPermission(resource = "user", action = "WRITE", idParam = "id")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return userAdminService.update(id, request);
    }

    @RequiresPermission(resource = "user", action = "DELETE", idParam = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
