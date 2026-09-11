package com.my.craft.security.service;

/** No {@code AdminRole} is registered for the given id. */
public class AdminRoleNotFoundException extends RuntimeException {

    public AdminRoleNotFoundException(Long id) {
        super("admin role not found: " + id);
    }
}
