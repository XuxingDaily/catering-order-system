package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .dishId(shoppingCartDTO.getDishId())
                .setmealId(shoppingCartDTO.getSetmealId())
                .dishFlavor(shoppingCartDTO.getDishFlavor())
                .build();

        // 搜索是否存在该项
        List<ShoppingCart> list = shoppingCartMapper.select(shoppingCart);

        // 如果存在，则查询，后更新 number + 1
        if (list != null && !list.isEmpty()) {
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);
            return;
        }

        // 否则，则插入新的项，number = 1
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();
        if (dishId != null) {
            // 被加入购物车的是菜品项
            Dish dish = dishMapper.selectById(dishId);
            shoppingCart.setName(dish.getName());
            shoppingCart.setAmount(dish.getPrice());
            shoppingCart.setImage(dish.getImage());
        } else {
            // 被加入购物车的是套餐项
            Setmeal setmeal = setmealMapper.selectById(setmealId);
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setAmount(setmeal.getPrice());
            shoppingCart.setImage(setmeal.getImage());
        }
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(shoppingCart);
    }

    /**
     * 查看购物车
     * @param userId
     * @return
     */
    @Override
    public List<ShoppingCart> list(Long userId) {
        return shoppingCartMapper.selectListByUserId(userId);
    }

    /**
     * 清空购物车
     * @param userId
     */
    @Override
    public void clean(Long userId) {
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中的一个商品
     * @param shoppingCartDTO
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .dishId(shoppingCartDTO.getDishId())
                .setmealId(shoppingCartDTO.getSetmealId())
                .dishFlavor(shoppingCartDTO.getDishFlavor())
                .build();

        List<ShoppingCart> list = shoppingCartMapper.select(shoppingCart);
        if (list != null && !list.isEmpty()) {
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() - 1);
            if (shoppingCart.getNumber() <= 0) {
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else {
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }

}
