package com.catering.trade.payment.refund.service;

import com.catering.entity.Orders;
import com.catering.trade.payment.refund.domain.RefundNotification;

/**
 * 退款申请与退款结果处理服务。
 */
public interface RefundService {

    /**
     * 为已支付且已取消的订单准备并发起全额退款。
     */
    void requestRefund(Orders order);

    /**
     * 校验并处理支付平台的退款最终结果。
     */
    void handleRefundResult(RefundNotification notification);

}
