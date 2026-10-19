package com.sky.catalog.dish.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入菜品的口味项
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 批量删除指定菜品id所属的口味项
     * @param dishIds
     */
    void deleteBatchByDishId(List<Long> dishIds);

    /**
     * 根据菜品id搜索菜品对应的口味
     * @param dishId
     * @return
     */
    List<DishFlavor> selectByDishId(Long dishId);

}
