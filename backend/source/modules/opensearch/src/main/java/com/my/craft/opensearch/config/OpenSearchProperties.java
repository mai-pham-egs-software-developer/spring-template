package com.my.craft.opensearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the top-level {@code opensearch:} YAML key.
 */
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

    /** OpenSearch host, e.g. {@code localhost}. */
    private String host = "localhost";

    /** OpenSearch HTTP port. */
    private int port = 9200;

    /** {@code http} or {@code https}. */
    private String scheme = "http";

    private String username;
    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
