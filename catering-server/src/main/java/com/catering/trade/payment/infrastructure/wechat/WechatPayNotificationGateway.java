package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSONObject;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.infrastructure.PaymentNotificationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信支付成功通知适配器。
 */
@Component
@ConditionalOnProperty(prefix = "catering.payment", name = "provider", havingValue = "wechat")
@RequiredArgsConstructor
public class WechatPayNotificationGateway implements PaymentNotificationGateway {

    private static final String SUCCESS_EVENT = "TRANSACTION.SUCCESS";
    private final WechatNotificationVerifier notificationVerifier;

    @Override
    public PaymentNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
        VerifiedWechatNotification verified = notificationVerifier.verifyAndDecrypt(
                serial, timestamp, nonce, signature, body
        );
        if (!SUCCESS_EVENT.equals(verified.getEventType())) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_EVENT_INVALID);
        }

        JSONObject transaction = verified.getResource();
        JSONObject amount = transaction.getJSONObject("amount");
        if (amount == null) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
        }
        PaymentNotification notification = PaymentNotification.builder()
                .eventId(verified.getEventId())
                .eventType(verified.getEventType())
                .appId(transaction.getString("appid"))
                .mchId(transaction.getString("mchid"))
                .outTradeNo(transaction.getString("out_trade_no"))
                .transactionId(transaction.getString("transaction_id"))
                .tradeState(transaction.getString("trade_state"))
                .totalAmount(amount.getInteger("total"))
                .currency(amount.getString("currency"))
                .successTime(transaction.getString("success_time"))
                .build();
        if (!StringUtils.hasText(notification.getAppId())
                || !StringUtils.hasText(notification.getMchId())
                || !StringUtils.hasText(notification.getOutTradeNo())
                || !StringUtils.hasText(notification.getTransactionId())
                || !StringUtils.hasText(notification.getTradeState())
                || notification.getTotalAmount() == null) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
        }
        return notification;
    }
}
