package com.catering.trade.order.mapper;

import com.catering.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细表数据访问接口。
 */
@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细。
     *
     * @param orderDetails 待保存的订单明细列表
     */
    void insertBatch(List<OrderDetail> orderDetails);

    /**
     * 根据订单 ID 查询全部明细。
     *
     * @param orderId 订单 ID
     * @return 订单明细列表
     */
    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    List<OrderDetail> selectByOrderId(Long orderId);
}
