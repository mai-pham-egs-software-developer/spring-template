package com.my.craft.opensearch.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables {@link OpenSearchProperties} so {@code com.my.craft.opensearch.SearchClient} can bind {@code opensearch.*}. */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {
}
