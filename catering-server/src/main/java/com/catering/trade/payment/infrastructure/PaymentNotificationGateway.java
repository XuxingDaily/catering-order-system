package com.catering.trade.payment.infrastructure;

import com.catering.trade.payment.pay.domain.PaymentNotification;

/**
 * 支付平台回调适配器。
 */
public interface PaymentNotificationGateway {

    /**
     * 校验回调签名并解密通知内容。
     *
     * @param serial 微信支付平台证书序列号
     * @param timestamp 微信回调时间戳
     * @param nonce 微信回调随机串
     * @param signature 微信回调签名
     * @param body 原始 HTTP 请求体，验签前不能改写
     * @return 标准化后的支付通知
     */
    PaymentNotification verifyAndDecrypt(
            String serial,
            String timestamp,
            String nonce,
            String signature,
            String body
    );
}
