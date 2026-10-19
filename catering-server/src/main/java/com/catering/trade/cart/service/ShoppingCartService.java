package com.catering.trade.cart.service;

import com.catering.dto.ShoppingCartDTO;
import com.catering.entity.ShoppingCart;

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

    /**
     * 删除购物车中的一个商品
     *
     * @param shoppingCartDTO
     */
    void sub(ShoppingCartDTO shoppingCartDTO);

}
