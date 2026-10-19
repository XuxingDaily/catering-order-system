package com.catering.catalog.setmeal.mapper;

import com.catering.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 批量插入套餐菜品关系项
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id批量删除项
     * @param setmealIds
     */
    void deleteBatchBySetmealId(List<Long> setmealIds);

    /**
     * 根据套餐id查询项
     * @param setmealId
     * @return
     */
    @Select("SELECT * FROM setmeal_dish WHERE setmeal_id = #{setmealId}")
    List<SetmealDish> selectBySetmealId(Long setmealId);

}
