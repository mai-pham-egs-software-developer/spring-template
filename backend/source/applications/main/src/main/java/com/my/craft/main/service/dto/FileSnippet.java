package com.my.craft.main.service.dto;

/** One highlighted fragment of a file: {@code chunk} is the 0-based chunk it comes from. */
public record FileSnippet(int chunk, String value) {}
