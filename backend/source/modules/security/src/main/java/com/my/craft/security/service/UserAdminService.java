package com.my.craft.security.service;

import java.util.List;

import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;

/**
 * CRUD over the persisted {@code User} -- named {@code UserAdminService}, not {@code
 * UserService}, to stay distinct from {@link com.my.craft.security.security.UserService} (the
 * current-principal-to-{@code UserContext} resolver; an unrelated concept that happens to share
 * the module).
 */
public interface UserAdminService {

    List<UserResponse> findAll();

    /** @throws UserNotFoundException if {@code id} doesn't exist. */
    UserResponse findById(String id);

    /** Writes the local row (status {@code PENDING}) and an outbox event in one transaction, then
     * returns immediately -- it never calls the identity provider itself. See {@code
     * com.my.craft.security.service.outbox.OutboxWorker} and {@code backend/docs/user-outbox.md}.
     *
     * @throws UserAlreadyExistsException if {@code request.username()}/{@code request.email()}
     *     already exists. */
    UserResponse create(CreateUserRequest request);

    /** @throws UserNotFoundException if {@code id} doesn't exist. */
    UserResponse update(String id, UpdateUserRequest request);

    /** @throws UserNotFoundException if {@code id} doesn't exist. */
    void delete(String id);
}
