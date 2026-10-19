package com.catering.catalog.category.service;

import com.catering.dto.CategoryDTO;
import com.catering.dto.CategoryPageQueryDTO;
import com.catering.entity.Category;
import com.catering.shared.result.PageResult;

import java.util.List;

public interface CategoryService {

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据类型查询分类
     * @return
     */
    List<Category> list(Integer type);

    /**
     * 新增分类
     * @param categoryDTO
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    void delete(Integer id);

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Integer id);

    /**
     * 修改分类
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

}
