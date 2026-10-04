package com.kvs.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user lacks permissions for an operation (HTTP 403).
 */
public class ForbiddenException extends BaseApiException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
