package com.catering.trade.payment.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.catering.entity.Orders;
import com.catering.properties.WeChatProperties;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderService;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.pay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Objects;

/**
 * 支付回调业务校验与订单支付结果处理。
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String TRADE_STATE_SUCCESS = "SUCCESS";
    private static final String CURRENCY_CNY = "CNY";

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final WeChatProperties weChatProperties;

    @Override
    public void handlePaymentSuccess(PaymentNotification notification) {
        validateMerchant(notification);
        if (!TRADE_STATE_SUCCESS.equals(notification.getTradeState())) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_EVENT_INVALID);
        }

        Orders order = orderMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getNumber, notification.getOutTradeNo())
                        .last("limit 1")
        );
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        validateAmount(order, notification);

        // 原子状态迁移和重复回调识别继续由现有订单状态服务负责。
        orderService.paySuccess(notification.getOutTradeNo(), notification.getTransactionId());
    }

    private void validateMerchant(PaymentNotification notification) {
        if (!Objects.equals(weChatProperties.getAppid(), notification.getAppId())
                || !Objects.equals(weChatProperties.getMchid(), notification.getMchId())) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_MERCHANT_INVALID);
        }
    }

    private void validateAmount(Orders order, PaymentNotification notification) {
        if (order.getAmount() == null
                || notification.getTotalAmount() == null
                || !CURRENCY_CNY.equals(notification.getCurrency())) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_AMOUNT_INVALID);
        }

        try {
            int expectedAmount = order.getAmount()
                    .movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .intValueExact();
            if (expectedAmount != notification.getTotalAmount()) {
                throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_AMOUNT_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_AMOUNT_INVALID, ex);
        }
    }
}
