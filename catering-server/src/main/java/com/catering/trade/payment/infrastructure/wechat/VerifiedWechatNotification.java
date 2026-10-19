package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 已验签并解密的微信支付通知内部模型。
 */
@Getter
@AllArgsConstructor
public class VerifiedWechatNotification {

    private final String eventId;
    private final String eventType;
    private final JSONObject resource;
}
