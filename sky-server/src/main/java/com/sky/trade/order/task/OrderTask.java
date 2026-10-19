package com.sky.trade.order.task;

import com.sky.shared.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.trade.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        // 检测是否有超时的订单
        Integer status = Orders.PENDING_PAYMENT;
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(-15);

        List<Orders> timeoutOrders = orderMapper.selectByStatusAndEndTime(status, endTime);
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }

        for (Orders order : timeoutOrders) {
            order.setStatus(Orders.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason(MessageConstant.ORDER_TIMEOUT);
            orderMapper.update(order);
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrder() {
        log.info("凌晨一点处理派送中订单: {}", LocalDateTime.now());

        // 要求是昨天派送的
        Integer status = Orders.PENDING_PAYMENT;
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(-60);

        List<Orders> deliveryOrders = orderMapper.selectByStatusAndEndTime(status, endTime);
        if (deliveryOrders == null || deliveryOrders.isEmpty()) {
            return;
        }

        for (Orders order : deliveryOrders) {
            order.setStatus(Orders.COMPLETED);
            orderMapper.update(order);
        }
    }

}
