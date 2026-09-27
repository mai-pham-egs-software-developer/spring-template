package com.my.craft.opensearch.index;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;

/** Reads a {@link Document}/{@link Field}-annotated class and builds its index name + mapping. */
public final class IndexMappingFactory {

    private IndexMappingFactory() {}

    public static String indexNameOf(Class<?> documentType) {
        Document document = documentType.getAnnotation(Document.class);
        if (document == null) {
            throw new IllegalArgumentException(documentType.getName() + " is not annotated with @Document");
        }
        return document.index();
    }

    /** Walks {@code documentType} and its superclasses for {@link Field}-annotated fields to build the mapping. */
    public static CreateIndexRequest createIndexRequest(Class<?> documentType) {
        String index = indexNameOf(documentType);
        Map<String, Property> properties = new LinkedHashMap<>();
        for (Class<?> type = documentType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field declaredField : type.getDeclaredFields()) {
                if (Modifier.isStatic(declaredField.getModifiers())) {
                    continue;
                }
                Field mapping = declaredField.getAnnotation(Field.class);
                if (mapping != null) {
                    properties.putIfAbsent(declaredField.getName(), toProperty(mapping.type()));
                }
            }
        }
        return CreateIndexRequest.of(c -> c.index(index).mappings(TypeMapping.of(m -> m.properties(properties))));
    }

    private static Property toProperty(FieldType type) {
        return switch (type) {
            case TEXT -> Property.of(p -> p.text(t -> t));
            case KEYWORD -> Property.of(p -> p.keyword(k -> k));
            case LONG -> Property.of(p -> p.long_(l -> l));
            case INTEGER -> Property.of(p -> p.integer(i -> i));
            case DOUBLE -> Property.of(p -> p.double_(d -> d));
            case BOOLEAN -> Property.of(p -> p.boolean_(b -> b));
            case DATE -> Property.of(p -> p.date(d -> d));
        };
    }
}
