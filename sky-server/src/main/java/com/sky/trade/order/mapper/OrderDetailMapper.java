package com.sky.trade.order.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单详细项
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);

    /**
     * 根据订单id查询详细项
     * @param orderId
     * @return
     */
    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    List<OrderDetail> selectByOrderId(Long orderId);
}
