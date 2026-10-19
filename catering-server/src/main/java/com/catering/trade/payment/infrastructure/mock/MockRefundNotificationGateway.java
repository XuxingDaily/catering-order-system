package com.catering.trade.payment.infrastructure.mock;

import com.alibaba.fastjson.JSON;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.infrastructure.RefundNotificationGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 退款回调解析器，跳过真实微信验签和解密。
 */
@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockRefundNotificationGateway implements RefundNotificationGateway {

    @Override
    public RefundNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
        try {
            RefundNotification notification = JSON.parseObject(body, RefundNotification.class);
            if (notification == null) {
                throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
            }
            return notification;
        } catch (PaymentNotifyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID, ex);
        }
    }
}
