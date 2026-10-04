package com.kvs.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when client provides malformed or invalid request parameters (HTTP 400).
 */
public class BadRequestException extends BaseApiException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
