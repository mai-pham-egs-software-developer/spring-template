package com.my.craft.opensearch.index;

/** OpenSearch field types {@link IndexMappingFactory} knows how to map a {@link Field} to. */
public enum FieldType {
    TEXT,
    KEYWORD,
    LONG,
    INTEGER,
    DOUBLE,
    BOOLEAN,
    DATE
}
