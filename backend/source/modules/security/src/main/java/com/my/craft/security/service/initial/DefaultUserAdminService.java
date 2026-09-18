package com.my.craft.security.service.initial;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.auditlog.service.AuditActionRecorder;
import com.my.craft.security.domain.OutboxEvent;
import com.my.craft.security.domain.User;
import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;
import com.my.craft.security.repository.OutboxEventJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;
import com.my.craft.security.service.UserAdminService;
import com.my.craft.security.service.UserAlreadyExistsException;
import com.my.craft.security.service.UserNotFoundException;

@Service
public class DefaultUserAdminService implements UserAdminService {

    private final UserJpaRepository userRepository;
    private final OutboxEventJpaRepository outboxRepository;
    private final AuditActionRecorder auditActionRecorder;

    public DefaultUserAdminService(
            UserJpaRepository userRepository, OutboxEventJpaRepository outboxRepository, AuditActionRecorder auditActionRecorder) {
        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.auditActionRecorder = auditActionRecorder;
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    public UserResponse findById(String id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User user;
        try {
            user = userRepository.save(new User(request.username(), request.email(), request.name()));
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException(request.username(), request.email());
        }
        // Same transaction as the insert above -- either both commit, or neither does, which is
        // what makes the async Keycloak sync (OutboxWorker) safe to retry without ever losing or
        // duplicating the request.
        outboxRepository.save(new OutboxEvent(
                user.getId(),
                OutboxEvent.USER_CREATE_REQUESTED,
                Map.of("username", request.username(), "email", request.email(), "name", request.name())));
        auditActionRecorder.record("USER_CREATED", user);
        return UserResponse.from(user);
    }

    @Override
    public UserResponse update(String id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        user.setName(request.name());
        User saved = userRepository.save(user);
        auditActionRecorder.record("USER_UPDATED", saved);
        return UserResponse.from(saved);
    }

    @Override
    public void delete(String id) {
        User user = findOrThrow(id);
        userRepository.deleteById(id);
        auditActionRecorder.recordDeletion("USER_DELETED", user);
    }

    private User findOrThrow(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
