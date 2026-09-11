package com.my.craft.security.dto;

import com.my.craft.security.domain.operator.AdminRole;
import com.my.craft.security.domain.User;

/** API-facing shape for {@link User} -- keeps the JPA entity out of the wire format. */
public record UserResponse(String id, String name, Long roleId, String roleName) {

    /** Reads {@code user.getRole()} -- caller must still be inside the transaction/session
     * {@code user} was loaded in ({@code role} is {@code FetchType.LAZY} and {@code
     * open-in-view} is off), or this throws {@code LazyInitializationException}. */
    public static UserResponse from(User user) {
        AdminRole role = user.getRole();
        return new UserResponse(user.getId(), user.getName(), role != null ? role.getId() : null, role != null ? role.getName() : null);
    }
}
