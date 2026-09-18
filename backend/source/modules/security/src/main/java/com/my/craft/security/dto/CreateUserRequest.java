package com.my.craft.security.dto;

/** {@code username}/{@code email} create the account in the identity provider (its generated id
 * becomes {@code User.id} -- see {@code UserService.createUser}); {@code name} is this app's own
 * display name, stored locally alongside it. */
public record CreateUserRequest(String username, String email, String name) {}
