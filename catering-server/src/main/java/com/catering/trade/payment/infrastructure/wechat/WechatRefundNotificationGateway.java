package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSONObject;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.infrastructure.RefundNotificationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信退款结果通知适配器。
 */
@Component
@ConditionalOnProperty(prefix = "catering.payment", name = "provider", havingValue = "wechat")
@RequiredArgsConstructor
public class WechatRefundNotificationGateway implements RefundNotificationGateway {

    private static final String REFUND_EVENT_PREFIX = "REFUND.";
    private final WechatNotificationVerifier notificationVerifier;

    @Override
    public RefundNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    ) {
        VerifiedWechatNotification verified = notificationVerifier.verifyAndDecrypt(
                serial, timestamp, nonce, signature, body
        );
        if (!StringUtils.hasText(verified.getEventType())
                || !verified.getEventType().startsWith(REFUND_EVENT_PREFIX)) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_EVENT_INVALID);
        }

        JSONObject refund = verified.getResource();
        JSONObject amount = refund.getJSONObject("amount");
        if (amount == null) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
        }
        RefundNotification notification = RefundNotification.builder()
                .eventId(verified.getEventId())
                .eventType(verified.getEventType())
                .mchId(refund.getString("mchid"))
                .orderNumber(refund.getString("out_trade_no"))
                .transactionId(refund.getString("transaction_id"))
                .refundNo(refund.getString("out_refund_no"))
                .refundId(refund.getString("refund_id"))
                .refundStatus(refund.getString("refund_status"))
                .refundAmount(amount.getInteger("refund"))
                // 微信退款结果通知的 amount 对象不返回 currency，国内退款固定按 CNY 校验。
                .currency("CNY")
                .successTime(refund.getString("success_time"))
                .build();
        if (!StringUtils.hasText(notification.getMchId())
                || !StringUtils.hasText(notification.getOrderNumber())
                || !StringUtils.hasText(notification.getTransactionId())
                || !StringUtils.hasText(notification.getRefundNo())
                || !StringUtils.hasText(notification.getRefundStatus())
                || notification.getRefundAmount() == null) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_BODY_INVALID);
        }
        return notification;
    }
}
