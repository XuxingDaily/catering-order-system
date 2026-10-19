package com.catering.trade.cart.mapper;

import com.catering.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 根据 userId 查询购物车列表
     * @param userId
     * @return
     */
    @Select("SELECT * FROM shopping_cart WHERE user_id = #{userId}")
    List<ShoppingCart> selectListByUserId(Long userId);

    /**
     * 条件查询购物车项
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> select(ShoppingCart shoppingCart);

    /**
     * 根据id更新购物车项的数量
     * @param shoppingCart
     */
    @Update("UPDATE shopping_cart SET number = #{number} WHERE id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 插入新购物车项
     * @param shoppingCart
     */
    @Insert("INSERT INTO shopping_cart(name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) VALUES " +
            "(#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id清空购物车
     * @param userId
     */
    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根据id删除购物车项
     * @param id
     */
    @Delete("DELETE FROM shopping_cart WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 批量插入购物车项
     * @param shoppingCarts
     */
    void insertBatch(List<ShoppingCart> shoppingCarts);
}
