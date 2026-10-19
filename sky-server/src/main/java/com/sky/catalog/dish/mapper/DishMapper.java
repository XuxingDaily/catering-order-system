package com.sky.catalog.dish.mapper;

import com.github.pagehelper.Page;
import com.sky.shared.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
