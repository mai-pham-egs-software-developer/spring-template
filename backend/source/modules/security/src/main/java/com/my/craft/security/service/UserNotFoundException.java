package com.my.craft.security.service;

/** No {@code User} is registered for the given (local, app-generated) id. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String id) {
        super("user not found: " + id);
    }
}
