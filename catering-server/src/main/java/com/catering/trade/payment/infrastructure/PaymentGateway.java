package com.catering.trade.payment.infrastructure;

import com.catering.vo.OrderPaymentVO;

import java.math.BigDecimal;

/**
 * 支付平台预下单网关，屏蔽真实微信支付与本地 Mock 的实现差异。
 */
public interface PaymentGateway {

    /**
     * 创建预支付交易并返回前端调起支付所需参数。
     */
    OrderPaymentVO createPayment(
            String orderNumber,
            BigDecimal amount,
            String description,
            String openid
    );

}
