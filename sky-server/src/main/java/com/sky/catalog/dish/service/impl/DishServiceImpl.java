package com.sky.catalog.dish.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.catalog.dish.mapper.DishFlavorMapper;
import com.sky.catalog.dish.mapper.DishMapper;
import com.sky.shared.result.PageResult;
import com.sky.catalog.dish.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<Dish> dishes = dishMapper.selectList(dishPageQueryDTO);

        long total = dishes.getTotal();
        List<Dish> records = dishes.getResult();

        return new PageResult(total, records);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        DishPageQueryDTO dishPageQueryDTO = new DishPageQueryDTO();
        dishPageQueryDTO.setCategoryId(Integer.valueOf(categoryId.toString()));

        return dishMapper.selectList(dishPageQueryDTO);
    }

    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {
        List<Dish> dishes = this.list(categoryId);

        // 查询每个 dish 对应的 flavors
        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);

            // 查询 flavors
            List<DishFlavor> dishFlavors = dishFlavorMapper.selectByDishId(dish.getId());
            if (!dishFlavors.isEmpty()) {
                dishVO.setFlavors(dishFlavors);
            }

            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 修改菜品状态
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.update(dish);
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改 dish
        dishMapper.update(dish);

        // 修改 dish_flavor
        // 1. 批量删除 dishId 为 dish.id 的项
        // 2. 添加 dishDTO 中的 flavors
        List<Long> ids = new ArrayList<>();
        ids.add(dish.getId());
        dishFlavorMapper.deleteBatchByDishId(ids);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据id查询菜品及其口味
     * @param id
     * @return
     */
    @Override
    public DishVO getById(Long id) {
        // 搜索菜品项
        Dish dish = dishMapper.selectById(id);

        // 搜索菜品对应的口味项
        List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(id);

        // 封装为 VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 删除菜品本身
        dishMapper.deleteBatch(ids);

        // 删除菜品附带的口味项
        dishFlavorMapper.deleteBatchByDishId(ids);
    }

    /**
     * 新增菜品以及对应的口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);

        // id 回填后获取
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

}
