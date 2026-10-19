package com.sky.catalog.setmeal.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.catalog.setmeal.mapper.SetmealDishMapper;
import com.sky.catalog.setmeal.mapper.SetmealMapper;
import com.sky.shared.result.PageResult;
import com.sky.catalog.setmeal.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImlp implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNo = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNo, pageSize);
        Page<Setmeal> page = setmealMapper.selectList(setmealPageQueryDTO);

        long total = page.getTotal();
        List<Setmeal> list = page.getResult();

        return new PageResult(total, list);
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        List<Long> ids = new ArrayList<>();
        ids.add(setmeal.getId());
        setmealDishMapper.deleteBatchBySetmealId(ids);

        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            setmealDishList.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmeal.getId());
            });
            setmealDishMapper.insertBatch(setmealDishList);
        }
    }

    /**
     * 根据ID查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 删除 setmeal 中项
        setmealMapper.deleteBatch(ids);

        // 删除 setmeal_dish 中项
        setmealDishMapper.deleteBatchBySetmealId(ids);
    }

    /**
     * 根据分类id查询套餐
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> getListByCategoryId(Setmeal setmeal) {
        return setmealMapper.selectListByCategoryId(setmeal);
    }

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.selectDishItemBySetmealId(id);
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 插入套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();

        // 插入套餐菜品关系
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            setmealDishList.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishMapper.insertBatch(setmealDishList);
        }
    }

    /**
     * 修改套餐状态（起售、停售）
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        setmealMapper.update(setmeal);
    }

}
