package com.my.craft.main.service.dto;

import java.util.List;

/** One file matching the keyword, with highlighted snippets from its matching chunks. */
public record FileSearchItem(String fileRefId, String subject, String documentType, List<FileSnippet> snippets) {}
