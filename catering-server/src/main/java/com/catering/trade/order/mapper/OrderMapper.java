package com.catering.trade.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.catering.dto.GoodsSalesDTO;
import com.catering.dto.OrderStatusCountDTO;
import com.catering.dto.OrdersPageQueryDTO;
import com.catering.entity.Orders;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单主表数据访问接口。
 *
 * <p>通用单表操作由 MyBatis-Plus {@link BaseMapper} 提供，分页与聚合统计等
 * 复杂查询保留为专用 SQL。</p>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 按用户端或管理端查询条件分页检索订单。
     *
     * @param ordersPageQueryDTO 查询条件
     * @return 订单列表
     */
    Page<Orders> selectPageByCondition(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各订单状态的订单数量。
     *
     * @return 各个状态的订单数量
     */
    @Select("SELECT status, COUNT(*) AS count FROM orders GROUP BY status")
    List<OrderStatusCountDTO> countByStatus();

    /**
     * 根据状态和时间区间统计营业额。
     *
     * @param beginTime 起始时间
     * @param endTime 终止时间
     * @param status 订单状态
     * @return 营业额
     */
    @MapKey("orderDate")
    List<Map<String, Object>> getTurnoverStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer status);

    /**
     * 根据日期区间和状态统计订单量。
     *
     * @param beginTime 起始时间
     * @param endTime 终止时间
     * @param status 订单状态
     * @return 订单统计
     */
    @MapKey("orderDate")
    List<Map<String, Object>> getOrderStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer status);

    /**
     * 根据日期区间和状态统计销量前十名。
     *
     * @param beginTime 起始时间
     * @param endTime 终止时间
     * @param status 订单状态
     * @return 销量前十名
     */
    List<GoodsSalesDTO> getSalesStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer status);

}
