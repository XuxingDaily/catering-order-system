package com.catering.shared.exception;

public class PaymentBusinessException extends RuntimeException {
    public PaymentBusinessException(String message) {
        super(message);
    }
}
