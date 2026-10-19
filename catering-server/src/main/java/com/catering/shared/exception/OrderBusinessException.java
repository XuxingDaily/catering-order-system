package com.catering.shared.exception;

/**
 * 订单领域业务异常。
 *
 * <p>用于表示订单不存在、状态不允许流转、支付或退款前置条件不满足等
 * 可预期业务错误，并由全局异常处理器统一转换为接口响应。</p>
 */
public class OrderBusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用面向客户端的全局消息常量创建异常。
     *
     * @param msg 业务错误消息
     */
    public OrderBusinessException(String msg) {
        super(msg);
    }

}
