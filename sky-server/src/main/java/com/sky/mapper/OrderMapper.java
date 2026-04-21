package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    /**
     * 插入新的订单项
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders selectByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 条件查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> selectList(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单项
     * @param id
     * @return
     */
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Orders selectById(Long id);
}
