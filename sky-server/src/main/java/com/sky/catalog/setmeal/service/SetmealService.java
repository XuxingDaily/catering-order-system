package com.sky.catalog.setmeal.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.shared.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据ID查询套餐
     * @param id
     * @return
     */
    SetmealVO getById(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);

    /**
     * 修改套餐状态（起售、停售）
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Long id);

    /**
     * 根据分类id查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> getListByCategoryId(Setmeal setmeal);

    /**
     *根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
