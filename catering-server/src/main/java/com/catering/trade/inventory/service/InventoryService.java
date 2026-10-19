package com.catering.trade.inventory.service;

import com.catering.entity.OrderDetail;

import java.util.List;

public interface InventoryService {

    /**
     * 下单时扣减库存
     * @param orderDetails 菜品列表
     */
    void deduct(List<OrderDetail> orderDetails);

    /**
     * 取消订单时释放库存
     * @param orderDetails 菜品列表
     */
    void release(List<OrderDetail> orderDetails);

}
