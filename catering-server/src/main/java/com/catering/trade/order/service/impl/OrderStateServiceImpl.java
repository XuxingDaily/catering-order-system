package com.catering.trade.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.catering.entity.Orders;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.trade.order.domain.OrderEvent;
import com.catering.trade.order.domain.OrderStatus;
import com.catering.trade.order.domain.OrderTransition;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 订单状态服务实现。
 *
 * <p>所有状态迁移都先校验迁移规则，再以原状态作为数据库更新条件，
 * 防止支付回调、取消请求和定时任务并发执行时覆盖彼此的结果。</p>
 */
@Service
@RequiredArgsConstructor
public class OrderStateServiceImpl implements OrderStateService {

    private final OrderMapper orderMapper;

    /**
     * 根据当前数据库状态解析迁移规则，并使用原状态作为更新条件完成 CAS 迁移。
     */
    @Override
    public void transition(Long orderId, OrderEvent event) {
        Orders order = getOrder(orderId);
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        OrderTransition transition = OrderTransition.resolve(status, event);

        // 原状态参与 WHERE 条件，避免并发请求基于过期状态重复迁移。
        int affected = orderMapper.update(
                null,
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, transition.getSource().getCode())
                        .set(Orders::getStatus, transition.getTarget().getCode())
        );
        checkAffected(affected);
    }

    /**
     * 原子处理支付成功回调，并通过目标状态与微信交易号识别重复通知。
     */
    @Override
    public boolean paySuccess(String orderNumber, String transactionId, LocalDateTime checkoutTime) {
        OrderTransition transition = OrderTransition.PAY_SUCCESS;
        // 条件性更新，幂等的核心
        int affected = orderMapper.update(
                null,
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getNumber, orderNumber)
                        .eq(Orders::getStatus, transition.getSource().getCode())
                        .eq(Orders::getPayStatus, PayStatus.UN_PAID.getCode())
                        .set(Orders::getStatus, transition.getTarget().getCode())
                        .set(Orders::getPayStatus, PayStatus.PAID.getCode())         // 更新支付状态
                        .set(Orders::getTransactionId, transactionId)                // 填入支付交易号
                        .set(Orders::getCheckoutTime, checkoutTime)
        );
        if (affected == 1) {
            return true;
        }

        // 更新失败可能是重复回调，也可能是非法状态，回查订单后再区分。
        Orders current = orderMapper.selectOne(
                Wrappers.<Orders>lambdaQuery().eq(Orders::getNumber, orderNumber)
        );
        if (current == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 微信可能重复通知。只有订单号、交易号和目标状态都一致时才视为幂等成功。
        if (Objects.equals(current.getStatus(), transition.getTarget().getCode())
                && Objects.equals(current.getPayStatus(), PayStatus.PAID.getCode())
                && Objects.equals(current.getTransactionId(), transactionId)) {
            return false;
        }
        throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    /**
     * 执行用户取消、管理员取消或商家拒单，并同步记录取消业务字段。
     */
    @Override
    public void cancel(Long orderId, OrderEvent event, String reason, LocalDateTime cancelTime) {
        // 只允许取消类事件进入该方法，防止普通状态事件误写取消字段。
        if (event != OrderEvent.USER_CANCEL
                && event != OrderEvent.ADMIN_CANCEL
                && event != OrderEvent.MERCHANT_REJECT) {
            throw new OrderBusinessException(MessageConstant.ORDER_CANCEL_EVENT_INVALID);
        }

        Orders order = getOrder(orderId);
        OrderTransition transition = OrderTransition.resolve(OrderStatus.fromCode(order.getStatus()), event);

        LambdaUpdateWrapper<Orders> wrapper =
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, transition.getSource().getCode())
                        .set(Orders::getStatus, transition.getTarget().getCode())
                        .set(Orders::getCancelTime, cancelTime);

        if (event == OrderEvent.MERCHANT_REJECT) {
            wrapper.set(Orders::getRejectionReason, reason);
        } else {
            wrapper.set(Orders::getCancelReason, reason);
        }

        if (transition.getSource() == OrderStatus.PENDING_PAYMENT) {
            // 未支付取消无需退款；支付状态同时作为并发保护条件。
            wrapper.eq(Orders::getPayStatus, PayStatus.UN_PAID.getCode());
        } else {
            // 已支付订单先进入退款中，最终结果由退款响应或异步回调确认。
            wrapper.eq(Orders::getPayStatus, PayStatus.PAID.getCode())
                    .set(Orders::getRefundStatus, RefundStatus.PROCESSING.getCode());
        }

        checkAffected(orderMapper.update(null, wrapper));
    }

    /**
     * 尝试关闭超时未支付订单；若支付回调先完成，CAS 更新会返回失败。
     */
    @Override
    public boolean timeoutCancel(Long orderId, String reason, LocalDateTime cancelTime) {
        OrderTransition transition = OrderTransition.TIMEOUT_CANCEL;
        return orderMapper.update(
                null,
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, transition.getSource().getCode())
                        .eq(Orders::getPayStatus, PayStatus.UN_PAID.getCode())
                        .set(Orders::getStatus, transition.getTarget().getCode())
                        .set(Orders::getCancelReason, reason)
                        .set(Orders::getCancelTime, cancelTime)
        ) == 1;
    }

    /**
     * 将退款中的订单更新为最终结果，并将重复的相同退款结果视为幂等成功。
     */
    @Override
    public void finishRefund(Long orderId, RefundStatus result, LocalDateTime refundTime) {
        if (result != RefundStatus.SUCCESS && result != RefundStatus.FAILED) {
            throw new OrderBusinessException(MessageConstant.REFUND_RESULT_INVALID);
        }

        LambdaUpdateWrapper<Orders> wrapper = Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, OrderStatus.CANCELLED.getCode())
                        .eq(Orders::getRefundStatus, RefundStatus.PROCESSING.getCode())
                        .set(Orders::getRefundStatus, result.getCode());
        if (result == RefundStatus.SUCCESS) {
            wrapper.set(Orders::getRefundTime, refundTime);
        }

        int affected = orderMapper.update(null, wrapper);
        if (affected == 1) {
            return;
        }

        // 微信可能重复通知；数据库已经是相同结果时无需再次更新。
        Orders current = getOrder(orderId);
        if (Objects.equals(current.getRefundStatus(), result.getCode())) {
            return;
        }
        throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    /**
     * 将配送中订单更新为已完成，并在同一条 SQL 中记录实际送达时间。
     */
    @Override
    public void complete(Long orderId, LocalDateTime deliveryTime) {
        OrderTransition transition = OrderTransition.COMPLETE;
        int affected = orderMapper.update(
                null,
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, transition.getSource().getCode())
                        .set(Orders::getStatus, transition.getTarget().getCode())
                        .set(Orders::getDeliveryTime, deliveryTime)
        );
        checkAffected(affected);
    }

    /**
     * 查询订单，不存在时统一抛出订单业务异常。
     */
    private Orders getOrder(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 校验 CAS 更新结果；影响行数不是 1 表示订单状态已被其他请求改变。
     */
    private void checkAffected(int affected) {
        if (affected != 1) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }
}
