package com.my.craft.opensearch;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.craft.opensearch.config.OpenSearchProperties;

import jakarta.annotation.PreDestroy;

/**
 * Holds the low-level {@link OpenSearchTransport}/{@link OpenSearchClient} connection, built from
 * {@code opensearch.*} ({@link OpenSearchProperties}). Callers use {@link #raw()} to issue
 * requests. Wrapped in {@link LoggingOpenSearchTransport} so every request's method/path/raw JSON
 * body can be logged at DEBUG (enable {@code com.my.craft.opensearch: DEBUG}) without the noise
 * of httpclient5's own byte-level wire logging.
 */
@Component
public class SearchClient {

    private final OpenSearchTransport transport;
    private final OpenSearchClient client;

    public SearchClient(OpenSearchProperties properties) {
        HttpHost host = new HttpHost(properties.getScheme(), properties.getHost(), properties.getPort());
        // FAIL_ON_UNKNOWN_PROPERTIES off: a search against com.my.craft.opensearch.index.BaseIndexDocument
        // (or any other shared supertype) hits documents whose subclass added fields the supertype
        // doesn't declare -- those should be silently dropped, not blow up the whole search.
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ApacheHttpClient5TransportBuilder builder =
                ApacheHttpClient5TransportBuilder.builder(host).setMapper(new JacksonJsonpMapper(objectMapper));
        if (properties.getUsername() != null && properties.getPassword() != null) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(host),
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword().toCharArray()));
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        }
        this.transport = new LoggingOpenSearchTransport(builder.build());
        this.client = new OpenSearchClient(transport);
    }

    /** The underlying client for issuing OpenSearch requests. */
    public OpenSearchClient raw() {
        return client;
    }

    @PreDestroy
    public void close() throws Exception {
        transport.close();
    }
}
