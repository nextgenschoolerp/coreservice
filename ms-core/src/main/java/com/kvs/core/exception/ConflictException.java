package com.kvs.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation conflicts with the current resource state (e.g. duplicate key / email) (HTTP 409).
 */
public class ConflictException extends BaseApiException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS");
    }

    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
