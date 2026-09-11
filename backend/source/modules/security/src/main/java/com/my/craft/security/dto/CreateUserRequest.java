package com.my.craft.security.dto;

/** {@code id} is the Keycloak subject claim ({@code sub}) -- see {@code User}. */
public record CreateUserRequest(String id, String name, Long roleId) {}
