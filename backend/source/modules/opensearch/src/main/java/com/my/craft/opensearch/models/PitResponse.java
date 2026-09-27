package com.my.craft.opensearch.models;

import java.util.List;

/**
 * One page of a point-in-time paged search. {@code pitId} is the cursor for the next page, or
 * {@code null} when there are no more results (the snapshot is then already closed). {@code
 * remainingDuration} is how many seconds the cursor stays valid if not used; {@code 0} once closed.
 */
public record PitResponse<T>(List<T> data, String pitId, long remainingDuration) {}
