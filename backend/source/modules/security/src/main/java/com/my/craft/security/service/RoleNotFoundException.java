package com.my.craft.security.service;

/** No {@code Role} is registered for the given id, scoped to its organization. */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long id) {
        super("role not found: " + id);
    }
}
