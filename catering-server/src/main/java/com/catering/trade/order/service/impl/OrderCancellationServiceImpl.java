package com.catering.trade.order.service.impl;

import com.catering.entity.Orders;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.trade.inventory.service.InventoryService;
import com.catering.trade.order.domain.OrderEvent;
import com.catering.trade.order.mapper.OrderDetailMapper;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderCancellationService;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 订单取消编排服务实现。
 *
 * <p>集中处理用户取消、管理员取消、商家拒单和超时关单，并在订单已支付时
 * 衔接退款流程。库存释放将在库存模块接入后补充。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final OrderMapper orderMapper;
    private final OrderStateService orderStateService;
    private final OrderDetailMapper orderDetailMapper;
    private final InventoryService inventoryService;
    private final RefundService refundService;

    /**
     * 用户取消自己的订单：先校验订单归属，再执行状态迁移及必要的退款申请。
     */
    @Override
    public void cancelByUser(Long orderId, Long userId) {
        Orders order = getOrder(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        orderStateService.cancel(
                orderId,
                OrderEvent.USER_CANCEL,
                MessageConstant.USER_CANCEL_ORDER,
                LocalDateTime.now()
        );

        // 未支付订单会直接返回；已支付订单进入退款流程。
        applyRefundIfNecessary(orderId);

        // 在取消成功后统一释放本订单占用的菜品/套餐库存。
        inventoryService.release(orderDetailMapper.selectByOrderId(orderId));
    }

    /**
     * 管理员取消订单。管理员不校验订单归属，但仍受状态迁移规则约束。
     */
    @Override
    public void cancelByAdmin(Long orderId, String reason) {
        orderStateService.cancel(
                orderId,
                OrderEvent.ADMIN_CANCEL,
                reason,
                LocalDateTime.now()
        );

        // 未支付订单会直接返回；已支付订单进入退款流程。
        applyRefundIfNecessary(orderId);

        // 在取消成功后统一释放本订单占用的菜品/套餐库存。
        inventoryService.release(orderDetailMapper.selectByOrderId(orderId));
    }

    /**
     * 定时任务尝试关闭超时未支付订单；并发支付已完成时不重复取消。
     */
    @Override
    public void cancelByTimeout(Long orderId) {
        boolean cancelled = orderStateService.timeoutCancel(
                orderId,
                MessageConstant.ORDER_TIMEOUT,
                LocalDateTime.now()
        );
        if (cancelled) {
            // 在超时关单成功后统一释放本订单占用的菜品/套餐库存。
            inventoryService.release(orderDetailMapper.selectByOrderId(orderId));
        }
    }

    /**
     * 商家拒绝待接单订单，并为已支付订单发起退款。
     */
    @Override
    public void rejectByMerchant(Long orderId, String reason) {
        orderStateService.cancel(
                orderId,
                OrderEvent.MERCHANT_REJECT,
                reason,
                LocalDateTime.now()
        );

        // 拒单发生在支付成功之后，因此正常情况下会进入退款流程。
        applyRefundIfNecessary(orderId);

        // 在拒单成功后统一释放本订单占用的菜品/套餐库存。
        inventoryService.release(orderDetailMapper.selectByOrderId(orderId));
    }

    /**
     * 对已支付订单申请退款。退款平台调用将在当前取消事务提交后执行。
     */
    private void applyRefundIfNecessary(Long orderId) {
        // 状态迁移后重新查询，退款判断以数据库中的最新支付、退款状态为准。
        Orders order = getOrder(orderId);
        log.info("取消订单后的退款判断：orderId={}, orderNumber={}, payStatus={}, transactionId={}, refundStatus={}, refundNo={}",
                order.getId(), order.getNumber(), order.getPayStatus(), order.getTransactionId(),
                order.getRefundStatus(), order.getRefundNo());

        // 只有已支付订单才需要退款。
        if (!Objects.equals(order.getPayStatus(), PayStatus.PAID.getCode())) {
            log.info("订单未支付，无需退款：orderId={}, payStatus={}", order.getId(), order.getPayStatus());
            return;
        }

        // 微信退款需要支付成功回调保存的 transactionId。
        if (order.getTransactionId() == null || order.getTransactionId().isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_TRANSACTION_ID_MISSING);
        }

        refundService.requestRefund(order);
    }

    /**
     * 查询待取消订单并统一处理不存在场景。
     */
    private Orders getOrder(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }
}
