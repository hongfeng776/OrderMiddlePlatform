package com.orderplatform.common.exception;

public class IdempotentException extends RuntimeException {

    public IdempotentException(String message) {
        super(message);
    }
}
