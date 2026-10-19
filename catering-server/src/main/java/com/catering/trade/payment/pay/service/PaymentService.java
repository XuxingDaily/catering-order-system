package com.catering.trade.payment.pay.service;

import com.catering.trade.payment.pay.domain.PaymentNotification;

/**
 * 支付结果业务服务。
 */
public interface PaymentService {

    /**
     * 校验并处理支付成功通知。
     *
     * @param notification 已验签、解密的支付通知
     */
    void handlePaymentSuccess(PaymentNotification notification);
}
