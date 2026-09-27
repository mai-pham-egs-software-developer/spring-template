package com.my.craft.opensearch.models.pit;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.exception.OpenSearchOperationException;

/** Opening/deleting OpenSearch PITs, shared by {@link PointInTimePaging} and {@link SearchAfterPaging}. */
final class PitLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PitLifecycle.class);

    private PitLifecycle() {}

    static String open(SearchClient searchClient, String index, long ttlSeconds) {
        try {
            return searchClient.raw()
                    .createPit(r -> r.index(index).keepAlive(t -> t.time(keepAlive(ttlSeconds))))
                    .pitId();
        } catch (IOException e) {
            throw new OpenSearchOperationException("failed to open point in time on index " + index, e);
        }
    }

    /** Best effort: an undeleted PIT still expires on its own after its keep-alive. */
    static void close(SearchClient searchClient, String openSearchPitId) {
        try {
            searchClient.raw().deletePit(d -> d.pitId(openSearchPitId));
        } catch (IOException | RuntimeException e) {
            log.warn("failed to delete point in time, it will expire on its own", e);
        }
    }

    static String keepAlive(long ttlSeconds) {
        return ttlSeconds + "s";
    }
}
