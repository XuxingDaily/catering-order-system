package com.catering.trade.payment.refund.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 本地取消事务提交后，通知支付平台发起退款的领域事件。
 */
@Getter
@RequiredArgsConstructor
public class RefundRequestedEvent {

    private final RefundRequest request;
}
