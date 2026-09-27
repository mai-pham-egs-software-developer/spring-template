package com.my.craft.opensearch.exception;

/** A search or bulk request failed at the transport level (connection, timeout, serialization). */
public class OpenSearchOperationException extends RuntimeException {

    public OpenSearchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
