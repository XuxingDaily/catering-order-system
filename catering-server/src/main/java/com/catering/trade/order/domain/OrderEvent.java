package com.catering.trade.order.domain;

/**
 * 驱动订单状态迁移的业务事件。
 */
public enum OrderEvent {

    PAY_SUCCESS,            // 支付成功
    USER_CANCEL,            // 用户取消
    ADMIN_CANCEL,           // 管理员取消
    TIMEOUT_CANCEL,         // 超时取消
    MERCHANT_CONFIRM,       // 商家接单
    MERCHANT_REJECT,        // 商家拒单
    START_DELIVERY,         // 开始配送
    COMPLETE                // 配送完成

}
