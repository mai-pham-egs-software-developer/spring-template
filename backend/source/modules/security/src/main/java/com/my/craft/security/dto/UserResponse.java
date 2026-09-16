package com.my.craft.security.dto;

import com.my.craft.security.domain.User;

/** API-facing shape for {@link User} -- keeps the JPA entity out of the wire format. */
public record UserResponse(String id, String name) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName());
    }
}
