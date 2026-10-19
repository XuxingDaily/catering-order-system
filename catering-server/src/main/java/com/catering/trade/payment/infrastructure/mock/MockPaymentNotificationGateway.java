package com.catering.trade.payment.infrastructure.mock;

import com.alibaba.fastjson.JSON;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.infrastructure.PaymentNotificationGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 回调解析器。仅在本地 Mock 支付模式启用，跳过微信证书验签和报文解密。
 */
@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockPaymentNotificationGateway implements PaymentNotificationGateway {

    @Override
    public PaymentNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
        try {
            PaymentNotification notification = JSON.parseObject(body, PaymentNotification.class);
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
