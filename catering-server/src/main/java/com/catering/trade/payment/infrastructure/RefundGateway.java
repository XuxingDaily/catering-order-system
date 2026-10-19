package com.catering.trade.payment.infrastructure;

import com.catering.trade.payment.refund.domain.RefundRequest;

/**
 * 支付平台退款申请网关。
 */
public interface RefundGateway {

    void requestRefund(RefundRequest request);
}
