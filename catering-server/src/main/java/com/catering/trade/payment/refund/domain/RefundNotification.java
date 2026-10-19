package com.catering.trade.payment.refund.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已完成协议层验签、解密后的退款结果通知。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundNotification {

    private String eventId;
    private String eventType;
    private String mchId;
    private String orderNumber;
    private String transactionId;
    private String refundNo;
    private String refundId;
    private String refundStatus;
    private Integer refundAmount;
    private String currency;
    private String successTime;
}
