package com.uniqdata.backend.core;

public class CoreClientException extends RuntimeException {

    public CoreClientException(String message) {
        super(message);
    }

    public CoreClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
