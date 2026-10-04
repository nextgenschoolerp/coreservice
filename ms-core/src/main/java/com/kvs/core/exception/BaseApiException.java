package com.kvs.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base abstract exception for all custom domain exceptions in KVS microservices.
 */
@Getter
public abstract class BaseApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected BaseApiException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    protected BaseApiException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
