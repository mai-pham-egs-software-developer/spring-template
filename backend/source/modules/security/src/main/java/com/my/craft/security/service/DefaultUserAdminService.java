package com.my.craft.security.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;
import com.my.craft.security.domain.operator.AdminRole;
import com.my.craft.security.domain.User;
import com.my.craft.security.repository.AdminRoleJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;

/**
 * {@code role} is {@code FetchType.LAZY} on {@link User} and {@code open-in-view} is off, so
 * every method that reads it to build a {@link UserResponse} needs to stay inside a transaction
 * for the whole read+map -- {@code @Transactional} here does that; the old controller-level
 * placement of the same annotation moved down into its rightful layer.
 */
@Service
public class DefaultUserAdminService implements UserAdminService {

    private final UserJpaRepository userRepository;
    private final AdminRoleJpaRepository roleRepository;

    public DefaultUserAdminService(UserJpaRepository userRepository, AdminRoleJpaRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User user = new User(request.id(), request.name());
        user.setRole(resolveRole(request.roleId()));
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(String id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        user.setName(request.name());
        user.setRole(resolveRole(request.roleId()));
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void delete(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private User findOrThrow(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private AdminRole resolveRole(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return roleRepository.findById(roleId).orElseThrow(() -> new AdminRoleNotFoundException(roleId));
    }
}
