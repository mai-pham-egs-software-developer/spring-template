package com.my.craft.security.dto;

import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserStatus;

/** API-facing shape for {@link User} -- keeps the JPA entity out of the wire format. {@code
 * status} tracks the async, outbox-driven sync to the identity provider (see
 * {@code backend/docs/user-outbox.md}): {@code PENDING} right after {@code create}, {@code
 * ACTIVE} once {@code OutboxWorker} links {@code keycloakId}, or {@code CONFLICT}/{@code FAILED}
 * if it needs a human. */
public record UserResponse(String id, String username, String email, String name, UserStatus status) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName(), user.getStatus());
    }
}
