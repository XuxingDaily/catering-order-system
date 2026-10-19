package com.catering.trade.payment.pay.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum PayStatus {

    UN_PAID(0, "未支付"),
    PAID(1, "已支付");

    private final Integer code;
    private final String description;

}
