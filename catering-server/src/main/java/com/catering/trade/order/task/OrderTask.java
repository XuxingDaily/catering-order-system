package com.catering.trade.order.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.catering.entity.Orders;
import com.catering.trade.order.domain.OrderStatus;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderCancellationService;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务，负责兜底关闭超时未支付订单以及完成长期配送中的订单。
 *
 * <p>定时扫描只负责发现候选订单，实际状态变更统一委托状态/取消服务，
 * 由数据库条件更新处理任务重复执行和并发操作。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {

    private final OrderMapper orderMapper;
    private final OrderStateService orderStateService;
    private final OrderCancellationService orderCancellationService;

    /**
     * 每分钟扫描并关闭已超过支付截止时间的待支付订单。
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        // 检测是否有超时的订单
        Integer status = OrderStatus.PENDING_PAYMENT.getCode();
        LocalDateTime now = LocalDateTime.now();

        // 搜索超时订单
        List<Orders> timeoutOrders = orderMapper.selectList(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getStatus, status)
                        .eq(Orders::getPayStatus, PayStatus.UN_PAID.getCode())
                        .le(Orders::getExpireTime, now)
        );
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }

        for (Orders order : timeoutOrders) {
            // 扫描结果可能已经被支付线程处理，取消服务通过状态条件更新解决竞争。
            // 异常处理，防止一次异常后，剩余项不超时取消订单了。
            // TODO：（支付平台订单查询）往往还需要去根据商户订单号 outTradeNo 去向微信查询这笔支付单当前是已支付、未支付，还是已关闭？如果未支付则调用支付平台关闭订单
            try {
                orderCancellationService.cancelByTimeout(order.getId());
            } catch (Exception e) {
                log.error("关闭超时订单失败，orderId={}", order.getId(), e);
            }
        }
    }

    /**
     * 每天凌晨一点兜底完成长时间处于配送中的订单。
     */
    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrder() {
        log.info("凌晨一点处理派送中订单: {}", LocalDateTime.now());

        // 当前按下单时间超过一小时筛选，后续可在表中增加配送开始时间后改用该字段。
        Integer status = OrderStatus.DELIVERY_IN_PROGRESS.getCode();
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(-60);

        List<Orders> deliveryOrders = orderMapper.selectList(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getStatus, status)
                        .lt(Orders::getOrderTime, endTime)
        );
        if (deliveryOrders == null || deliveryOrders.isEmpty()) {
            return;
        }

        for (Orders order : deliveryOrders) {
            orderStateService.complete(order.getId(), LocalDateTime.now());
        }
    }

}
