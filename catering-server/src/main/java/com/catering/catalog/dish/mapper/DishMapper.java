package com.catering.catalog.dish.mapper;

import com.github.pagehelper.Page;
import com.catering.shared.annotation.AutoFill;
import com.catering.dto.DishPageQueryDTO;
import com.catering.entity.Dish;
import com.catering.shared.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<Dish> selectList(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据分类id查询菜品名称
     * @param categoryId
     * @return
     */
    @Select("SELECT COUNT(id) FROM dish WHERE category_id = #{categoryId}")
    Integer countByCategoryId(Integer categoryId);

    /**
     * 插入新菜品
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 修改菜品
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据id搜索菜品
     * @param id
     * @return
     */
    Dish selectById(Long id);

    /**
     * 根据状态统计菜品数量
     * @param status
     * @return
     */
    @Select("SELECT COUNT(1) FROM dish WHERE status = #{status}")
    Integer getDishStatisticsByStatus(Integer status);

    /**
     * 条件性扣除库存
     * @param dishId 菜品 id
     * @param quantity 扣除数量
     * @return 影响的记录行数
     */
    @Update("""
        UPDATE dish
        SET stock = stock - #{quantity}
        WHERE id = #{dishId}
          AND status = 1
          AND stock >= #{quantity}
    """)
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 条件性释放库存
     * @param dishId 菜品 id
     * @param quantity 释放数量
     * @return 影响的记录行数
     */
    @Update("""
        UPDATE dish
        SET stock = stock + #{quantity}
        WHERE id = #{dishId}
    """)
    int releaseStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

}
