package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    public BusinessDataVO getBusinessData() {
        LocalDate now = LocalDate.now();
        LocalDateTime beginTime = now.atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);

        // 新增用户数
        Integer newUsers = userMapper.selectUserByTime(beginTime, endTime);

        // 营业额
        List<Map<String, Object>> turnoverResultMap = orderMapper.getTurnoverStatistics(beginTime, endTime, Orders.COMPLETED);
        BigDecimal turnover = turnoverResultMap != null && !turnoverResultMap.isEmpty() ?
                (BigDecimal) turnoverResultMap.get(0).get("turnover") : new BigDecimal("0.0");

        // 已完成的订单数
        int completedOrders = getOrderCountByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);

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
    public OrderOverViewVO getOrderOverview() {
        LocalDate now = LocalDate.now();
        LocalDateTime beginTime = now.atStartOfDay();
        LocalDateTime endTime = beginTime.plusDays(1);

        // 待接单
        int waitingOrders = getOrderCountByTimeAndStatus(beginTime, endTime, Orders.TO_BE_CONFIRMED);

        // 待派送
        int deliveredOrders = getOrderCountByTimeAndStatus(beginTime, endTime, Orders.CONFIRMED);

        // 已完成
        int completedOrders = getOrderCountByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);

        // 已取消
        int cancelledOrders = getOrderCountByTimeAndStatus(beginTime, endTime, Orders.CANCELLED);

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
