package com.catering.trade.order.domain;

import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 订单主状态。
 *
 * <p>状态码与 {@code orders.status} 保持一致，业务代码应使用枚举而不是散落的数字常量。</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    PENDING_PAYMENT(1, "待支付"),
    TO_BE_CONFIRMED(2, "待接单"),
    CONFIRMED(3, "已接单"),
    DELIVERY_IN_PROGRESS(4, "配送中"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消");

    private final Integer code;
    private final String description;

    /**
     * 根据数据库状态码解析订单状态。
     *
     * @param code 数据库存储的状态码
     * @return 对应的订单状态
     * @throws OrderBusinessException 状态码为空或无法识别时抛出
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_REQUIRED);
        }

        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new OrderBusinessException(
                        MessageConstant.ORDER_STATUS_UNKNOWN + "：" + code
                ));
    }

}
