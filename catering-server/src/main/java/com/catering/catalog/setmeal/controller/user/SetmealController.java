package com.catering.catalog.setmeal.controller.user;

import com.catering.shared.constant.StatusConstant;
import com.catering.entity.Setmeal;
import com.catering.shared.result.Result;
import com.catering.catalog.setmeal.service.SetmealService;
import com.catering.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController("UserSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "套餐浏览接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类id查询起售的套餐
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    @Cacheable(cacheNames = "setmealCache", key = "#categoryId")
    public Result<List<Setmeal>> list(Long categoryId) {
        log.info("根据分类id查询起售的套餐 {}", categoryId);

        // 设置限制：状态为起售的套餐
        Setmeal setmeal = Setmeal.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();

        List<Setmeal> setmeals = setmealService.getListByCategoryId(setmeal);
        return Result.success(setmeals);
    }

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品")
    public Result<List<DishItemVO>> dish(@PathVariable Long id) {
        log.info("根据套餐id查询包含的菜品 {}", id);
        List<DishItemVO> dishItemVOs = setmealService.getDishItemById(id);
        return Result.success(dishItemVOs);
    }

}
