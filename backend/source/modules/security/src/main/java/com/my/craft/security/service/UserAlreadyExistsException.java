package com.my.craft.security.service;

/** Thrown when {@code username}/{@code email} already exists locally (a DB unique constraint
 * violation) -- the race-condition guard for two concurrent creates of the same username: since
 * both the {@code User} row and its {@code OutboxEvent} are written in one transaction (see
 * {@code DefaultUserAdminService.create}), only one request can ever win. */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username, String email) {
        super("user already exists: username='%s', email='%s'".formatted(username, email));
    }
}
