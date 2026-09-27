package com.my.craft.main.service.dto;

import java.util.List;

/**
 * One page of a scroll-paged search. {@code scrollId} is the cursor for the next page, {@code
 * null} when there are no more results; {@code remainingDuration} is how many seconds it stays
 * valid if unused.
 */
public record ScrollPage<T>(List<T> data, String scrollId, long remainingDuration) {}
