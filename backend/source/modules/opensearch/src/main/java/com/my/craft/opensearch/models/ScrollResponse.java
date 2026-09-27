package com.my.craft.opensearch.models;

import java.util.List;

/**
 * One page of a scroll-paged search. {@code scrollId} is the cursor for the next page, or {@code
 * null} when there are no more results (the scroll context is then already cleared). {@code
 * remainingDuration} is how many seconds the cursor stays valid if not used; {@code 0} once cleared.
 */
public record ScrollResponse<T>(List<T> data, String scrollId, long remainingDuration) {}
