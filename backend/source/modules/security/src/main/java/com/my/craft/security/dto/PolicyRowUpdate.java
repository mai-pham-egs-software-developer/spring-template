package com.my.craft.security.dto;

import java.util.List;

public record PolicyRowUpdate(String ptype, List<String> oldParams, List<String> newParams) {}
