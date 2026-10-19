package com.catering.trade.payment.infrastructure;

import com.catering.trade.payment.refund.domain.RefundNotification;

/**
 * 支付平台退款通知适配器。
 */
public interface RefundNotificationGateway {

    RefundNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    );
}
