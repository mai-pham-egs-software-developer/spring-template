package com.my.craft.main.service.dto;

import java.util.List;

/**
 * One page of a point-in-time paged search. {@code pitId} is the cursor for the next page, {@code
 * null} when there are no more results; {@code remainingDuration} is how many seconds it stays
 * valid if unused.
 */
public record PitPage<T>(List<T> data, String pitId, long remainingDuration) {}
