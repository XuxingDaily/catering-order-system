package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    /**
     * 查看购物车
     * @param userId
     * @return
     */
    List<ShoppingCart> list(Long userId);

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 清空购物车
     * @param userId
     */
    void clean(Long userId);

}
