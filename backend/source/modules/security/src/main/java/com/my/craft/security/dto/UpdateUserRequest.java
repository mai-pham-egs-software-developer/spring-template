package com.my.craft.security.dto;

/** No {@code id} field -- it's immutable, taken from the path instead. */
public record UpdateUserRequest(String name) {}
