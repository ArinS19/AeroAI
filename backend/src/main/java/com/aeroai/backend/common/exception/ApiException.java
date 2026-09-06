package com.aeroai.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all deliberately-thrown application exceptions that map to
 * the Section 46.1 error envelope. Never carries a raw stack trace/message
 * into the client response beyond the sanitized {@code message} field.
 */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
