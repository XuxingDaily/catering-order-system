package com.catering.trade.payment.pay.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已完成验签和解密的支付成功通知。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotification {

    private String eventId;
    private String eventType;
    private String appId;
    private String mchId;
    private String outTradeNo;
    private String transactionId;
    private String tradeState;
    private Integer totalAmount;
    private String currency;
    private String successTime;
}
