package com.kvs.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when request lacks valid authentication credentials (HTTP 401).
 */
public class UnauthorizedException extends BaseApiException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
