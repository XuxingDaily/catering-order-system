package com.catering.shared.exception;

/**
 * 微信支付回调验签、解密或业务校验异常。
 */
public class PaymentNotifyException extends BaseException {

    private static final long serialVersionUID = 1L;

    public PaymentNotifyException(String message) {
        super(message);
    }

    public PaymentNotifyException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
