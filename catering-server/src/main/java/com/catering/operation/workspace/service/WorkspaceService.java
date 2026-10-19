package com.catering.operation.workspace.service;

import com.catering.vo.BusinessDataVO;
import com.catering.vo.DishOverViewVO;
import com.catering.vo.OrderOverViewVO;
import com.catering.vo.SetmealOverViewVO;

import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 查询今日运营数据
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 查询套餐总览
     * @return
     */
    SetmealOverViewVO getSetmealOverview();

    /**
     * 查询菜品总览
     * @return
     */
    DishOverViewVO getDishOverview();

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverview(LocalDateTime beginTime, LocalDateTime endTime);

}
