package com.my.craft.security.service;

/** No {@code Organization} is registered for the given id. */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(Long id) {
        super("organization not found: " + id);
    }
}
