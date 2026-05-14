package com.orderplatform.payment.exception;

public class RefundException extends RuntimeException {

    private String code;

    public RefundException(String message) {
        super(message);
        this.code = "REFUND_ERROR";
    }

    public RefundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
