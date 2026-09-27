package com.my.craft.opensearch.exception;

import com.my.craft.opensearch.service.OpenSearchService;
import org.opensearch.client.opensearch.core.BulkResponse;

/** At least one operation in a {@link OpenSearchService#bulkIngest} request failed. */
public class OpenSearchBulkException extends RuntimeException {

    private final transient BulkResponse response;

    public OpenSearchBulkException(BulkResponse response) {
        super(buildMessage(response));
        this.response = response;
    }

    /** The full response, including the per-item results, for callers that need to inspect failures. */
    public BulkResponse getResponse() {
        return response;
    }

    private static String buildMessage(BulkResponse response) {
        long failed = response.items().stream().filter(item -> item.error() != null).count();
        return "bulk request had %d failed item(s) out of %d".formatted(failed, response.items().size());
    }
}
