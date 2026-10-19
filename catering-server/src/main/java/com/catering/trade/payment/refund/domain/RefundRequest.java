package com.catering.trade.payment.refund.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 向支付平台发起退款所需的数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    private Long orderId;
    private String orderNumber;
    private String transactionId;
    private String refundNo;
    private BigDecimal refundAmount;
    private BigDecimal totalAmount;
}
