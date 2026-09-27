package com.my.craft.opensearch.models;

import org.opensearch.client.opensearch._types.query_dsl.Query;

/**
 * What to search for, independent of how results are paged. Each search defines its own criteria
 * (keyword + filters, a date range, ...) and turns it into an OpenSearch query here; {@link
 * KeywordSearchCriteria} covers the common "free text over some fields plus exact-match filters"
 * case.
 */
public interface SearchCriteria {

    Query toQuery();
}
