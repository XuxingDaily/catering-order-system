package com.catering.trade.order.domain;

import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 订单状态迁移表。
 *
 * <p>每个枚举项明确描述“原状态 + 事件 -> 目标状态”，用于集中约束合法流转。</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrderTransition {

    // 从待支付到待接单，对应支付成功事件
    PAY_SUCCESS(
            OrderStatus.PENDING_PAYMENT,
            OrderEvent.PAY_SUCCESS,
            OrderStatus.TO_BE_CONFIRMED
    ),

    // 从待支付到已取消，对应用户取消事件
    USER_CANCEL_UNPAID(
            OrderStatus.PENDING_PAYMENT,
            OrderEvent.USER_CANCEL,
            OrderStatus.CANCELLED
    ),

    // 从待接单到已取消，对应用户取消事件
    USER_CANCEL_PAID(
            OrderStatus.TO_BE_CONFIRMED,
            OrderEvent.USER_CANCEL,
            OrderStatus.CANCELLED
    ),

    ADMIN_CANCEL_UNPAID(
            OrderStatus.PENDING_PAYMENT,
            OrderEvent.ADMIN_CANCEL,
            OrderStatus.CANCELLED
    ),

    ADMIN_CANCEL_TO_BE_CONFIRMED(
            OrderStatus.TO_BE_CONFIRMED,
            OrderEvent.ADMIN_CANCEL,
            OrderStatus.CANCELLED
    ),

    ADMIN_CANCEL_CONFIRMED(
            OrderStatus.CONFIRMED,
            OrderEvent.ADMIN_CANCEL,
            OrderStatus.CANCELLED
    ),

    ADMIN_CANCEL_DELIVERING(
            OrderStatus.DELIVERY_IN_PROGRESS,
            OrderEvent.ADMIN_CANCEL,
            OrderStatus.CANCELLED
    ),

    // 从待支付到已取消，对应支付超时事件
    TIMEOUT_CANCEL(
            OrderStatus.PENDING_PAYMENT,
            OrderEvent.TIMEOUT_CANCEL,
            OrderStatus.CANCELLED
    ),

    // 从待接单到已接单，对应商家接单事件
    MERCHANT_CONFIRM(
            OrderStatus.TO_BE_CONFIRMED,
            OrderEvent.MERCHANT_CONFIRM,
            OrderStatus.CONFIRMED
    ),

    // 从待接单到已取消，对应商家拒单事件
    MERCHANT_REJECT(
            OrderStatus.TO_BE_CONFIRMED,
            OrderEvent.MERCHANT_REJECT,
            OrderStatus.CANCELLED
    ),

    // 从已接单到派送中，对应开始派送事件
    START_DELIVERY(
            OrderStatus.CONFIRMED,
            OrderEvent.START_DELIVERY,
            OrderStatus.DELIVERY_IN_PROGRESS
    ),

    // 从派送中到已完成，对应派送完成事件
    COMPLETE(
            OrderStatus.DELIVERY_IN_PROGRESS,
            OrderEvent.COMPLETE,
            OrderStatus.COMPLETED
    );

    private final OrderStatus source;
    private final OrderEvent event;
    private final OrderStatus target;

    /**
     * 根据当前状态和业务事件解析唯一的迁移规则。
     *
     * @param source 当前订单状态
     * @param event  触发事件
     * @return 匹配的迁移规则
     * @throws OrderBusinessException 当前状态不允许执行该事件时抛出
     */
    public static OrderTransition resolve(OrderStatus source, OrderEvent event) {

        return Arrays.stream(values())
                .filter(transition -> transition.source == source && transition.event == event)
                .findFirst()
                .orElseThrow(() -> new OrderBusinessException(
                        MessageConstant.ORDER_TRANSITION_NOT_ALLOWED
                                + "：" + source.getDescription() + " -> " + event
                ));
    }

}
