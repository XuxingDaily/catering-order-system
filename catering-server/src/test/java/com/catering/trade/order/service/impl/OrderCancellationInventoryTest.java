package com.catering.trade.order.service.impl;

import com.catering.entity.OrderDetail;
import com.catering.entity.Orders;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.trade.inventory.service.InventoryService;
import com.catering.trade.order.domain.OrderEvent;
import com.catering.trade.order.mapper.OrderDetailMapper;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.service.RefundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationInventoryTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderStateService orderStateService;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private RefundService refundService;

    @InjectMocks
    private OrderCancellationServiceImpl cancellationService;

    @Test
    void repeatedUserCancellationShouldReleaseStockOnlyOnce() {
        Long orderId = 100L;
        Long userId = 10L;
        Orders order = Orders.builder()
                .id(orderId)
                .userId(userId)
                .payStatus(PayStatus.UN_PAID.getCode())
                .build();
        List<OrderDetail> details = Collections.singletonList(
                OrderDetail.builder().orderId(orderId).dishId(1L).number(2).build()
        );
        when(orderMapper.selectById(orderId)).thenReturn(order);
        when(orderDetailMapper.selectByOrderId(orderId)).thenReturn(details);
        doNothing()
                .doThrow(new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR))
                .when(orderStateService)
                .cancel(
                        eq(orderId),
                        eq(OrderEvent.USER_CANCEL),
                        eq(MessageConstant.USER_CANCEL_ORDER),
                        any(LocalDateTime.class)
                );

        cancellationService.cancelByUser(orderId, userId);
        assertThrows(
                OrderBusinessException.class,
                () -> cancellationService.cancelByUser(orderId, userId)
        );

        verify(inventoryService, times(1)).release(details);
    }

    @Test
    void paidOrderCancellationShouldRequestRefund() {
        Long orderId = 101L;
        Long userId = 10L;
        Orders paidOrder = Orders.builder()
                .id(orderId)
                .userId(userId)
                .number("ORDER_101")
                .payStatus(PayStatus.PAID.getCode())
                .transactionId("TRANSACTION_101")
                .build();
        when(orderMapper.selectById(orderId)).thenReturn(paidOrder);
        when(orderDetailMapper.selectByOrderId(orderId)).thenReturn(Collections.emptyList());

        cancellationService.cancelByUser(orderId, userId);

        verify(refundService).requestRefund(paidOrder);
        verify(inventoryService).release(Collections.emptyList());
    }
}
