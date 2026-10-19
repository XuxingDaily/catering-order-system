package com.catering.trade.inventory.service.impl;

import com.catering.catalog.dish.mapper.DishMapper;
import com.catering.catalog.setmeal.mapper.SetmealMapper;
import com.catering.entity.OrderDetail;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.trade.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final DishMapper dishMapper;
    private final SetmealMapper setmealMapper;

    /**
     * 下单时扣减库存
     * @param orderDetails 菜品列表
     */
    @Override
    public void deduct(List<OrderDetail> orderDetails) {
        InventoryQuantity quantity = aggregate(orderDetails);

        // TreeMap 已按商品 ID 排序，保证并发下单时以相同顺序锁定数据库记录。
        for (Map.Entry<Long, Integer> entry : quantity.dishes.entrySet()) {
            int affected = dishMapper.deductStock(entry.getKey(), entry.getValue());
            if (affected != 1) {
                throw new OrderBusinessException(MessageConstant.DISH_STOCK_NOT_ENOUGH);
            }
        }

        for (Map.Entry<Long, Integer> entry : quantity.setmeals.entrySet()) {
            int affected = setmealMapper.deductStock(entry.getKey(), entry.getValue());
            if (affected != 1) {
                throw new OrderBusinessException(MessageConstant.SETMEAL_STOCK_NOT_ENOUGH);
            }
        }
    }

    /**
     * 取消订单时释放库存
     * @param orderDetails 菜品列表
     */
    @Override
    public void release(List<OrderDetail> orderDetails) {
        InventoryQuantity quantity = aggregate(orderDetails);

        for (Map.Entry<Long, Integer> entry : quantity.dishes.entrySet()) {
            int affected = dishMapper.releaseStock(entry.getKey(), entry.getValue());
            if (affected != 1) {
                throw new OrderBusinessException(MessageConstant.INVENTORY_RELEASE_FAILED);
            }
        }

        for (Map.Entry<Long, Integer> entry : quantity.setmeals.entrySet()) {
            int affected = setmealMapper.releaseStock(entry.getKey(), entry.getValue());
            if (affected != 1) {
                throw new OrderBusinessException(MessageConstant.INVENTORY_RELEASE_FAILED);
            }
        }
    }

    /**
     * 合并订单明细中相同菜品或套餐的购买数量。
     */
    private InventoryQuantity aggregate(List<OrderDetail> orderDetails) {
        Map<Long, Integer> dishes = new TreeMap<>();
        Map<Long, Integer> setmeals = new TreeMap<>();

        for (OrderDetail detail : orderDetails) {
            if (detail.getNumber() == null || detail.getNumber() <= 0) {
                throw new OrderBusinessException(MessageConstant.INVENTORY_QUANTITY_INVALID);
            }

            if (detail.getDishId() != null) {
                dishes.merge(detail.getDishId(), detail.getNumber(), Integer::sum);
            } else if (detail.getSetmealId() != null) {
                setmeals.merge(detail.getSetmealId(), detail.getNumber(), Integer::sum);
            } else {
                throw new OrderBusinessException(MessageConstant.INVENTORY_ITEM_INVALID);
            }
        }

        return new InventoryQuantity(dishes, setmeals);
    }

    private static class InventoryQuantity {
        private final Map<Long, Integer> dishes;
        private final Map<Long, Integer> setmeals;

        private InventoryQuantity(Map<Long, Integer> dishes, Map<Long, Integer> setmeals) {
            this.dishes = dishes;
            this.setmeals = setmeals;
        }
    }

}
