package com.catering.trade.order.service;

import com.catering.dto.*;
import com.catering.shared.result.PageResult;
import com.catering.vo.*;

/**
 * 面向订单 Controller 的应用服务门面。
 */
public interface OrderService {

    /**
     * 提交订单
     * @param ordersSubmitDTO 下单参数，包含客户端业务请求号
     * @return 已创建或幂等命中的订单信息
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO 支付参数
     * @return 拉起微信支付所需参数；当前环境返回模拟参数
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo   商户订单号
     * @param transactionId 微信支付交易号
     */
    void paySuccess(String outTradeNo, String transactionId);

    /**
     * 商户接单
     * @param ordersConfirmDTO 待接单订单信息
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 商户拒单
     * @param ordersRejectionDTO 订单 ID 与拒单原因
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 派送订单
     * @param id 已接单订单 ID
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id 配送中订单 ID
     */
    void complete(Long id);

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO 分页及筛选条件
     * @return 分页订单数据
     */
    PageResult getHistoryOrder(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单详细
     * @param id 订单 id
     * @return 订单具体信息
     */
    OrderVO getById(Long id);

    /**
     * 用户取消自己的订单。
     *
     * @param orderId 订单 ID
     */
    void cancelByUser(Long orderId);

    /**
     * 管理员取消订单。
     *
     * @param ordersCancelDTO 订单 ID 与取消原因
     */
    void cancelByAdmin(OrdersCancelDTO ordersCancelDTO);

    /**
     * 根据订单id再来一单
     * @param id 原订单 ID
     */
    void repetitionById(Long id);

    /**
     * 各个状态的订单数量统计
     * @return 待接单、待派送和配送中的订单数量
     */
    OrderStatisticsVO statisticsByStatus();

    /**
     * 用户催单
     * @param id 订单 ID
     */
    void reminder(Long id);

}
