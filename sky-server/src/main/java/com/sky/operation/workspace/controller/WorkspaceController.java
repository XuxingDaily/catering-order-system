package com.sky.operation.workspace.controller;

import com.sky.shared.result.Result;
import com.sky.operation.workspace.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台接口")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 查询今日运营数据
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("查询今日运营数据")
    public Result<BusinessDataVO> businessData() {
        log.info("查询今日运营数据");
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = today.atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);
        return Result.success(businessDataVO);
    }

    @GetMapping("/overviewSetmeals")
    @ApiOperation("查询套餐总览")
    public Result<SetmealOverViewVO> overviewSetmeals() {
        log.info("查询套餐总览");
        SetmealOverViewVO setmealOverViewVO = workspaceService.getSetmealOverview();
        return Result.success(setmealOverViewVO);
    }

    /**
     * 查询菜品总览
     * @return
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("查询菜品总览")
    public Result<DishOverViewVO> overviewDishes() {
        log.info("查询菜品总览");
        DishOverViewVO dishOverViewVO = workspaceService.getDishOverview();
        return Result.success(dishOverViewVO);
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查询订单管理数据")
    public Result<OrderOverViewVO> overviewOrders() {
        log.info("查询订单管理数据");
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = today.atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);
        OrderOverViewVO orderOverViewVO = workspaceService.getOrderOverview(beginTime, endTime);
        return Result.success(orderOverViewVO);
    }

}
