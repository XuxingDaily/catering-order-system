package com.catering.catalog.dish.controller.user;

import com.catering.shared.result.Result;
import com.catering.catalog.dish.service.DishService;
import com.catering.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController("UserDishController")
@RequestMapping("/user/dish")
@Api(tags = "菜品浏览接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品以及对应口味")
    @Cacheable(cacheNames = "dishCache", key = "#categoryId")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id查询菜品以及对应口味 {}", categoryId);
        List<DishVO> dishVOs = dishService.listWithFlavor(categoryId);
        return Result.success(dishVOs);
    }

}
