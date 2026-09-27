package com.my.craft.main.service.dto;

import java.util.List;

/** One PLAN or file; {@code title} is the plan's title or the file's subject. */
public record UnifiedSearchItem(String refId, SearchType type, String title, List<String> snippets) {}
