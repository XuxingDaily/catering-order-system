package com.catering.catalog.category.mapper;

import com.github.pagehelper.Page;
import com.catering.shared.annotation.AutoFill;
import com.catering.dto.CategoryPageQueryDTO;
import com.catering.entity.Category;
import com.catering.shared.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CategoryMapper {

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> selectList(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 新增分类
     * @param category
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Category category);

    /**
     * 根据id删除分类
     * @param id
     */
    void delete(Integer id);

    /**
     * 修改分类
     * @param category
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);

}
