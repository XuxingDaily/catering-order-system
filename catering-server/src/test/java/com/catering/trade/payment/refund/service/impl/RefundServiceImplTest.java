package com.catering.trade.payment.refund.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.catering.entity.Orders;
import com.catering.properties.WeChatProperties;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.refund.domain.RefundRequestedEvent;
import com.catering.trade.payment.refund.domain.RefundStatus;
import com.catering.trade.payment.refund.service.RefundServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Orders.class
        );
    }

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderStateService orderStateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private WeChatProperties weChatProperties;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    void requestRefundShouldPersistDeterministicRefundNoAndPublishEvent() {
        Orders order = paidOrder();
        when(orderMapper.update(eq(null), any(Wrapper.class))).thenReturn(1);

        refundService.requestRefund(order);

        ArgumentCaptor<RefundRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RefundRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("REFUND_ORDER_001", eventCaptor.getValue().getRequest().getRefundNo());
        assertEquals(new BigDecimal("18.80"), eventCaptor.getValue().getRequest().getRefundAmount());
    }

    @Test
    void successfulCallbackShouldFinishRefund() {
        Orders order = paidOrder();
        order.setRefundNo("REFUND_ORDER_001");
        when(weChatProperties.getMchid()).thenReturn("mock-mchid");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        RefundNotification notification = RefundNotification.builder()
                .mchId("mock-mchid")
                .orderNumber("ORDER_001")
                .transactionId("TRANSACTION_001")
                .refundNo("REFUND_ORDER_001")
                .refundStatus("SUCCESS")
                .refundAmount(1880)
                .currency("CNY")
                .build();

        refundService.handleRefundResult(notification);

        verify(orderStateService).finishRefund(
                eq(100L),
                eq(RefundStatus.SUCCESS),
                any(LocalDateTime.class)
        );
    }

    private Orders paidOrder() {
        return Orders.builder()
                .id(100L)
                .number("ORDER_001")
                .transactionId("TRANSACTION_001")
                .payStatus(PayStatus.PAID.getCode())
                .amount(new BigDecimal("18.80"))
                .build();
    }
}
