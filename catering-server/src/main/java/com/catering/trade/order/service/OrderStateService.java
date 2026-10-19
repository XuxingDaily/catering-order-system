package com.catering.trade.order.service;

import com.catering.trade.order.domain.OrderEvent;
import com.catering.trade.payment.refund.domain.RefundStatus;

import java.time.LocalDateTime;

/**
 * 订单状态迁移服务。
 *
 * <p>只负责校验迁移规则并通过数据库条件更新完成原子状态转换，不编排退款、库存等外围业务。</p>
 */
public interface OrderStateService {

    /**
     * 执行不携带额外业务字段的通用状态迁移。
     *
     * @param orderId 订单 ID
     * @param event   触发状态变化的事件
     */
    void transition(Long orderId, OrderEvent event);

    /**
     * 支付成功状态迁移。
     *
     * @param orderNumber  商户订单号
     * @param transactionId 微信支付交易号
     * @param checkoutTime  支付完成时间
     * @return {@code true} 表示首次处理；{@code false} 表示相同回调已处理
     */
    boolean paySuccess(String orderNumber, String transactionId, LocalDateTime checkoutTime);

    /**
     * 将订单迁移为已取消，并记录原因、时间及需要退款时的退款中状态。
     *
     * @param orderId   订单 ID
     * @param event     用户取消、管理员取消或商家拒单事件
     * @param reason    取消或拒单原因
     * @param cancelTime 取消时间
     */
    void cancel(Long orderId, OrderEvent event, String reason, LocalDateTime cancelTime);

    /**
     * 定时关单使用；订单已支付或已被其他请求处理时返回 {@code false}。
     *
     * @param orderId   订单 ID
     * @param reason    超时取消原因
     * @param cancelTime 取消时间
     * @return 是否由本次调用成功关闭订单
     */
    boolean timeoutCancel(Long orderId, String reason, LocalDateTime cancelTime);

    /**
     * 处理退款最终结果，可直接供微信退款回调复用并保证重复回调幂等。
     *
     * @param orderId 订单 ID
     * @param result  退款成功或退款失败
     * @param refundTime 退款完成时间
     */
    void finishRefund(Long orderId, RefundStatus result, LocalDateTime refundTime);

    /**
     * 完成配送中的订单并记录实际送达时间。
     *
     * @param orderId      订单 ID
     * @param deliveryTime 实际送达时间
     */
    void complete(Long orderId, LocalDateTime deliveryTime);

}
