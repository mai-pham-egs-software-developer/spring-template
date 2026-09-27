package com.my.craft.opensearch;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.transport.Endpoint;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.stream.JsonGenerator;

/**
 * Logs {@code method + path + raw JSON body} for every request, at DEBUG -- a clean alternative
 * to httpclient5's byte-level wire logging (escaped CRLFs, headers, response noise). Delegates
 * every actual call to {@code delegate}; this only observes.
 */
final class LoggingOpenSearchTransport implements OpenSearchTransport {

    private static final Logger log = LoggerFactory.getLogger(LoggingOpenSearchTransport.class);

    private final OpenSearchTransport delegate;

    LoggingOpenSearchTransport(OpenSearchTransport delegate) {
        this.delegate = delegate;
    }

    @Override
    public <RequestT, ResponseT, ErrorT> ResponseT performRequest(
            RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint, TransportOptions options) throws IOException {
        logRequest(request, endpoint);
        return delegate.performRequest(request, endpoint, options);
    }

    @Override
    public <RequestT, ResponseT, ErrorT> CompletableFuture<ResponseT> performRequestAsync(
            RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint, TransportOptions options) {
        logRequest(request, endpoint);
        return delegate.performRequestAsync(request, endpoint, options);
    }

    private <RequestT, ResponseT, ErrorT> void logRequest(RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint) {
        if (!log.isDebugEnabled()) {
            return;
        }
        String path = endpoint.requestUrl(request);
        String body = endpoint.hasRequestBody() ? serialize(request) : "";
        log.debug("OpenSearch {} {}\n{}", endpoint.method(request), path, body);
    }

    private String serialize(Object request) {
        try (StringWriter writer = new StringWriter()) {
            JsonpMapper mapper = delegate.jsonpMapper();
            JsonGenerator generator = mapper.jsonProvider().createGenerator(writer);
            mapper.serialize(request, generator);
            generator.close();
            return writer.toString();
        } catch (Exception e) {
            return "<unavailable: " + e.getMessage() + ">";
        }
    }

    @Override
    public JsonpMapper jsonpMapper() {
        return delegate.jsonpMapper();
    }

    @Override
    public TransportOptions options() {
        return delegate.options();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
