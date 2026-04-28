package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐数量
     * @param categoryId
     * @return
     */
    @Select("SELECT COUNT(id) FROM setmeal WHERE category_id = #{categoryId}")
    Integer countByCategoryId(Integer categoryId);

    /**
     * 分页查询
     * @return
     */
    Page<Setmeal> selectList(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 插入新增套餐
     * @param setmeal
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Select("SELECT * FROM setmeal WHERE id = #{id}")
    Setmeal selectById(Long id);

    /**
     * 修改套餐
     * @param setmeal
     */
    void update(Setmeal setmeal);

    /**
     * 根据分类id查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> selectListByCategoryId(Setmeal setmeal);

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    List<DishItemVO> selectDishItemBySetmealId(Long id);

    /**
     * 根据状态统计套餐量
     * @param status
     * @return
     */
    @Select("SELECT COUNT(1) FROM setmeal WHERE status = #{status}")
    Integer getSetmealStatisticsByStatus(Integer status);
}
