package com.my.craft.opensearch.index;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a mapped OpenSearch field; {@link IndexMappingFactory} reads it to build the index mapping. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Field {

    FieldType type();
}
