package com.catering.catalog.category.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.constant.StatusConstant;
import com.catering.dto.CategoryDTO;
import com.catering.dto.CategoryPageQueryDTO;
import com.catering.entity.Category;
import com.catering.shared.exception.DeletionNotAllowedException;
import com.catering.catalog.category.mapper.CategoryMapper;
import com.catering.catalog.dish.mapper.DishMapper;
import com.catering.catalog.setmeal.mapper.SetmealMapper;
import com.catering.shared.result.PageResult;
import com.catering.catalog.category.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(CategoryPageQueryDTO categoryPageQueryDTO) {
        int pageNo = categoryPageQueryDTO.getPage();
        int pageSize = categoryPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNo, pageSize);

        Page<Category> page = categoryMapper.selectList(categoryPageQueryDTO);

        long total = page.getTotal();
        List<Category> list = page.getResult();

        return new PageResult(total, list);
    }

    /**
     * 根据类型查询分类
     * @return
     */
    @Override
    public List<Category> list(Integer type) {
        CategoryPageQueryDTO categoryPageQueryDTO = new CategoryPageQueryDTO();
        categoryPageQueryDTO.setType(type);

        return categoryMapper.selectList(categoryPageQueryDTO);
    }

    /**
     * 新增分类
     * @param categoryDTO
     */
    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        // 状态默认禁止
        category.setStatus(StatusConstant.DISABLE);

        categoryMapper.insert(category);
    }

    /**
     * 根据id删除分类
     * @param id
     */
    @Override
    public void delete(Integer id) {
        // 查询该分类下的菜品数量、套餐数量，如果存在则不可以删除
        Integer numOfDish = dishMapper.countByCategoryId(id);
        if (numOfDish > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        Integer numOfSetmeal = setmealMapper.countByCategoryId(id);
        if (numOfSetmeal > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        categoryMapper.delete(id);
    }

    /**
     * 修改分类的状态
     * @param id
     * @param status
     */
    @Override
    public void updateStatus(Integer status, Integer id) {
        Category category = Category.builder().id(Long.valueOf(id)).status(status).build();

        categoryMapper.update(category);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        categoryMapper.update(category);
    }

}
