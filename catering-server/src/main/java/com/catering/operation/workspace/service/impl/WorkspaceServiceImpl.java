package com.catering.operation.workspace.service.impl;

import com.catering.shared.constant.StatusConstant;
import com.catering.entity.Orders;
import com.catering.catalog.dish.mapper.DishMapper;
import com.catering.trade.order.domain.OrderStatus;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.catalog.setmeal.mapper.SetmealMapper;
import com.catering.identity.user.mapper.UserMapper;
import com.catering.operation.workspace.service.WorkspaceService;
import com.catering.vo.BusinessDataVO;
import com.catering.vo.DishOverViewVO;
import com.catering.vo.OrderOverViewVO;
import com.catering.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    private int getOrderCountByTimeAndStatus(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        List<Map<String, Object>> resultMap = orderMapper.getOrderStatistics(beginTime, endTime, status);
        long count = resultMap != null && !resultMap.isEmpty() ? (long) resultMap.get(0).get("total") : 0L;
        return Math.toIntExact(count);
    }

    /**
     * 查询今日运营数据
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime beginTime, LocalDateTime endTime) {
        // 新增用户数
        Integer newUsers = userMapper.selectUserByTime(beginTime, endTime);

        // 营业额
        List<Map<String, Object>> turnoverResultMap = orderMapper.getTurnoverStatistics(beginTime, endTime, OrderStatus.COMPLETED.getCode());
        BigDecimal turnover = turnoverResultMap != null && !turnoverResultMap.isEmpty() ?
                (BigDecimal) turnoverResultMap.get(0).get("turnover") : new BigDecimal("0.0");

        // 已完成的订单数
        int completedOrders = getOrderCountByTimeAndStatus(beginTime, endTime, OrderStatus.COMPLETED.getCode());

        // 总订单数
        int allOrders = getOrderCountByTimeAndStatus(beginTime, endTime, null);

        // 计算订单完成率
        Double ratio = allOrders == 0 ? 1.0 : 1.0 * completedOrders / allOrders;

        // 计算平均客单价
        double unitPrice = completedOrders != 0 ? completedOrders * 1.0 / allOrders : 0.0;

        return BusinessDataVO.builder()
                .newUsers(newUsers)
                .orderCompletionRate(ratio)
                .turnover(Double.valueOf(turnover.toString()))
                .unitPrice(unitPrice)
                .validOrderCount(completedOrders)
                .build();
    }

    /**
     * 查询套餐总览
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverview() {
        Integer sold = setmealMapper.getSetmealStatisticsByStatus(StatusConstant.ENABLE);
        Integer discontinued = setmealMapper.getSetmealStatisticsByStatus(StatusConstant.DISABLE);
        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询菜品总览
     * @return
     */
    @Override
    public DishOverViewVO getDishOverview() {
        Integer sold = dishMapper.getDishStatisticsByStatus(StatusConstant.ENABLE);
        Integer discontinued = dishMapper.getDishStatisticsByStatus(StatusConstant.DISABLE);
        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverview(LocalDateTime beginTime, LocalDateTime endTime) {
        // 待接单
        int waitingOrders = getOrderCountByTimeAndStatus(beginTime, endTime, OrderStatus.TO_BE_CONFIRMED.getCode());

        // 待派送
        int deliveredOrders = getOrderCountByTimeAndStatus(beginTime, endTime, OrderStatus.CONFIRMED.getCode());

        // 已完成
        int completedOrders = getOrderCountByTimeAndStatus(beginTime, endTime, OrderStatus.COMPLETED.getCode());

        // 已取消
        int cancelledOrders = getOrderCountByTimeAndStatus(beginTime, endTime, OrderStatus.CANCELLED.getCode());

        // 全部订单
        int allOrders = getOrderCountByTimeAndStatus(beginTime, endTime, null);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

}
