package com.catering.trade.payment.refund.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {

    NONE(0, "无退款"),
    PROCESSING(1, "退款中"),
    SUCCESS(2, "退款成功"),
    FAILED(3, "退款失败");

    private final int code;
    private final String description;

}
