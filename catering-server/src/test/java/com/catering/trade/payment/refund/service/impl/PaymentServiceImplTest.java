package com.catering.trade.payment.refund.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.catering.entity.Orders;
import com.catering.properties.WeChatProperties;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderService;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.pay.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private WeChatProperties weChatProperties;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUpMerchant() {
        when(weChatProperties.getAppid()).thenReturn("wx-app-id");
        when(weChatProperties.getMchid()).thenReturn("merchant-id");
    }

    @Test
    void validNotificationShouldDelegateToOrderService() {
        PaymentNotification notification = validNotification(1880);
        Orders order = Orders.builder()
                .number("ORDER_001")
                .amount(new BigDecimal("18.80"))
                .build();
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        paymentService.handlePaymentSuccess(notification);

        verify(orderService).paySuccess("ORDER_001", "WX_TRANSACTION_001");
    }

    @Test
    void amountMismatchShouldRejectNotification() {
        PaymentNotification notification = validNotification(1881);
        Orders order = Orders.builder()
                .number("ORDER_001")
                .amount(new BigDecimal("18.80"))
                .build();
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        PaymentNotifyException exception = assertThrows(
                PaymentNotifyException.class,
                () -> paymentService.handlePaymentSuccess(notification)
        );

        assertEquals(MessageConstant.PAYMENT_NOTIFY_AMOUNT_INVALID, exception.getMessage());
        verify(orderService, never()).paySuccess(any(), any());
    }

    @Test
    void merchantMismatchShouldRejectBeforeQueryingOrder() {
        PaymentNotification notification = validNotification(1880);
        notification.setMchId("another-merchant");

        PaymentNotifyException exception = assertThrows(
                PaymentNotifyException.class,
                () -> paymentService.handlePaymentSuccess(notification)
        );

        assertEquals(MessageConstant.PAYMENT_NOTIFY_MERCHANT_INVALID, exception.getMessage());
        verify(orderMapper, never()).selectOne(any(Wrapper.class));
        verify(orderService, never()).paySuccess(any(), any());
    }

    private PaymentNotification validNotification(int totalAmount) {
        return PaymentNotification.builder()
                .eventType("TRANSACTION.SUCCESS")
                .appId("wx-app-id")
                .mchId("merchant-id")
                .outTradeNo("ORDER_001")
                .transactionId("WX_TRANSACTION_001")
                .tradeState("SUCCESS")
                .totalAmount(totalAmount)
                .currency("CNY")
                .build();
    }
}
