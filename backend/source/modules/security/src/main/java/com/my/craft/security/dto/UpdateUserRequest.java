package com.my.craft.security.dto;

/** No {@code id} field -- it's immutable, taken from the path instead. {@code roleId} null
 * unassigns the role. */
public record UpdateUserRequest(String name, Long roleId) {}
