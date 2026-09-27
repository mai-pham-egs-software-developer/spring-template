package com.my.craft.opensearch.index;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Common audit shape for anything stored in OpenSearch: the business key back to the source
 * record ({@code refId}) plus who/when it was created and last updated. Concrete indices (see
 * {@link Document}-annotated subclasses) extend this and add whatever searchable {@link Field}s
 * they actually hold (e.g. {@code title}/{@code content}) -- those are domain-specific, unlike
 * this audit trail, so they don't belong here.
 *
 * <p>{@code createdAt}/{@code lastUpdatedAt} are ISO-8601 strings (e.g. {@code
 * Instant.now().toString()}), not {@link java.time.Instant} -- the client's default {@code
 * JacksonJsonpMapper} isn't guaranteed to have the JSR-310 module registered, and a plain
 * ISO-8601 string already round-trips through Jackson with no extra wiring while still matching
 * OpenSearch's default {@code date} field format ({@code strict_date_optional_time}).
 *
 * <p>Deliberately not abstract: when a raw multi-index search spans indices backed by different
 * subclasses, pass {@code BaseIndexDocument.class} itself as the target type to get every hit back
 * as this common shape, instead of picking one subclass arbitrarily.
 */
@Getter
@Setter
@NoArgsConstructor
public class BaseIndexDocument {

    @Field(type = FieldType.KEYWORD)
    private String refId;

    @Field(type = FieldType.KEYWORD)
    private String createdId;

    @Field(type = FieldType.KEYWORD)
    private String updatedId;

    @Field(type = FieldType.DATE)
    private String createdAt;

    @Field(type = FieldType.DATE)
    private String lastUpdatedAt;

    protected BaseIndexDocument(String refId, String createdId, String updatedId, String createdAt, String lastUpdatedAt) {
        this.refId = refId;
        this.createdId = createdId;
        this.updatedId = updatedId;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
