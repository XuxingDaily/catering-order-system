package com.catering.trade.payment.refund.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.catering.entity.Orders;
import com.catering.properties.WeChatProperties;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.order.domain.OrderStatus;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.refund.domain.RefundRequest;
import com.catering.trade.payment.refund.domain.RefundRequestedEvent;
import com.catering.trade.payment.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 单订单单次全额退款实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private static final String REFUND_NO_PREFIX = "REFUND_";
    private static final String REFUND_SUCCESS = "SUCCESS";
    private static final String REFUND_ABNORMAL = "ABNORMAL";
    private static final String REFUND_CLOSED = "CLOSED";
    private static final String CURRENCY_CNY = "CNY";

    private final OrderMapper orderMapper;
    private final OrderStateService orderStateService;
    private final ApplicationEventPublisher eventPublisher;
    private final WeChatProperties weChatProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(Orders order) {
        if (!Objects.equals(order.getPayStatus(), PayStatus.PAID.getCode())) {
            return;
        }
        if (!StringUtils.hasText(order.getTransactionId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_TRANSACTION_ID_MISSING);
        }

        String refundNo = REFUND_NO_PREFIX + order.getNumber();
        log.info("开始登记退款申请：orderId={}, orderNumber={}, transactionId={}, refundNo={}",
                order.getId(), order.getNumber(), order.getTransactionId(), refundNo);
        int affected = orderMapper.update(
                null,
                Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getId, order.getId())
                        .eq(Orders::getStatus, OrderStatus.CANCELLED.getCode())
                        .eq(Orders::getRefundStatus, RefundStatus.PROCESSING.getCode())
                        .and(wrapper -> wrapper
                                .isNull(Orders::getRefundNo)
                                .or()
                                .eq(Orders::getRefundNo, refundNo)
                        )
                        .set(Orders::getRefundNo, refundNo)
        );
        if (affected != 1) {
            Orders current = orderMapper.selectById(order.getId());
            if (current == null
                    || !Objects.equals(current.getRefundStatus(), RefundStatus.PROCESSING.getCode())
                    || !Objects.equals(current.getRefundNo(), refundNo)) {
                throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
        }

        log.info("退款申请已登记，等待退款网关处理：orderId={}, refundNo={}", order.getId(), refundNo);

        RefundRequest request = RefundRequest.builder()
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .transactionId(order.getTransactionId())
                .refundNo(refundNo)
                .refundAmount(order.getAmount())
                .totalAmount(order.getAmount())
                .build();

        // 监听器在当前取消事务成功提交后才调用外部支付平台。
        eventPublisher.publishEvent(new RefundRequestedEvent(request));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundResult(RefundNotification notification) {
        if (notification == null || !StringUtils.hasText(notification.getRefundNo())) {
            throw new PaymentNotifyException(MessageConstant.REFUND_NO_MISSING);
        }
        if (!Objects.equals(weChatProperties.getMchid(), notification.getMchId())) {
            throw new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_MERCHANT_INVALID);
        }

        Orders order = orderMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getRefundNo, notification.getRefundNo())
                        .last("limit 1")
        );
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        validateNotificationOrder(order, notification);
        validateRefundAmount(order, notification);

        RefundStatus result;
        if (REFUND_SUCCESS.equals(notification.getRefundStatus())) {
            result = RefundStatus.SUCCESS;
        } else if (REFUND_ABNORMAL.equals(notification.getRefundStatus())
                || REFUND_CLOSED.equals(notification.getRefundStatus())) {
            result = RefundStatus.FAILED;
        } else {
            throw new PaymentNotifyException(MessageConstant.REFUND_RESULT_INVALID);
        }
        orderStateService.finishRefund(order.getId(), result, LocalDateTime.now());
    }

    private void validateNotificationOrder(Orders order, RefundNotification notification) {
        if (!Objects.equals(order.getNumber(), notification.getOrderNumber())
                || !Objects.equals(order.getTransactionId(), notification.getTransactionId())) {
            throw new PaymentNotifyException(MessageConstant.REFUND_ORDER_INVALID);
        }
    }

    private void validateRefundAmount(Orders order, RefundNotification notification) {
        if (order.getAmount() == null
                || notification.getRefundAmount() == null
                || !CURRENCY_CNY.equals(notification.getCurrency())) {
            throw new PaymentNotifyException(MessageConstant.REFUND_AMOUNT_INVALID);
        }
        try {
            int expectedAmount = order.getAmount().movePointRight(2).intValueExact();
            if (expectedAmount != notification.getRefundAmount()) {
                throw new PaymentNotifyException(MessageConstant.REFUND_AMOUNT_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new PaymentNotifyException(MessageConstant.REFUND_AMOUNT_INVALID, ex);
        }
    }
}
