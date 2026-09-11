package com.my.craft.security.service;

import java.util.List;

import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;

/**
 * CRUD over the persisted {@code User}/{@code AdminRole} assignment -- named {@code
 * UserAdminService}, not {@code UserService}, to stay distinct from {@link
 * com.my.craft.security.security.UserService} (the current-principal-to-{@code UserContext} resolver;
 * an unrelated concept that happens to share the module).
 */
public interface UserAdminService {

    List<UserResponse> findAll();

    /** @throws UserNotFoundException if {@code id} doesn't exist. */
    UserResponse findById(String id);

    /** @throws AdminRoleNotFoundException if {@code request.roleId()} is set but doesn't exist. */
    UserResponse create(CreateUserRequest request);

    /**
     * @throws UserNotFoundException if {@code id} doesn't exist.
     * @throws AdminRoleNotFoundException if {@code request.roleId()} is set but doesn't exist.
     */
    UserResponse update(String id, UpdateUserRequest request);

    /** @throws UserNotFoundException if {@code id} doesn't exist. */
    void delete(String id);
}
