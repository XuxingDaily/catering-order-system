package com.catering.trade.order.service;

/**
 * 订单取消业务编排服务。
 *
 * <p>负责权限校验、退款申请和库存释放等流程，实际状态迁移统一委托给
 * {@link OrderStateService}。</p>
 */
public interface OrderCancellationService {

    /**
     * 用户取消自己的订单。
     *
     * @param orderId 订单 ID
     * @param userId  当前登录用户 ID
     */
    void cancelByUser(Long orderId, Long userId);

    /**
     * 管理员取消处于可取消状态的订单。
     *
     * @param orderId 订单 ID
     * @param reason  取消原因
     */
    void cancelByAdmin(Long orderId, String reason);

    /**
     * 取消超时未支付订单；并发场景下状态已经改变时不重复处理。
     *
     * @param orderId 订单 ID
     */
    void cancelByTimeout(Long orderId);

    /**
     * 商家拒绝待接单订单。
     *
     * @param orderId 订单 ID
     * @param reason  拒单原因
     */
    void rejectByMerchant(Long orderId, String reason);

}
