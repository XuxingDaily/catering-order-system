package com.sky.controller.admin;

import com.sky.dto.ReportDTO;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.service.UserService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
public class ReportController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    /**
     * 营业额统计
     * @param reportDTO
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(ReportDTO reportDTO) {
        log.info("营业额统计，限定日期：{}", reportDTO);
        TurnoverReportVO turnoverReportVO = orderService.turnoverStatistics(reportDTO);
        return Result.success(turnoverReportVO);
    }

    /**
     * 用户统计
     * @param reportDTO
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(ReportDTO reportDTO) {
        log.info("用户统计，限定日期：{}", reportDTO);
        UserReportVO userReportVO = userService.userStatistics(reportDTO);
        return Result.success(userReportVO);
    }

    /**
     * 订单统计
     * @param reportDTO
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(ReportDTO reportDTO) {
        log.info("订单统计，限定日期：{}", reportDTO);
        OrderReportVO orderReportVO = orderService.orderStatistics(reportDTO);
        return Result.success(orderReportVO);
    }

    /**
     * 查询销量排名top10接口
     * @param reportDTO
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("查询销量排名top10接口")
    public Result<SalesTop10ReportVO> top10(ReportDTO reportDTO) {
        log.info("查询销量排名top10接口，限定日期：{}", reportDTO);
        SalesTop10ReportVO salesTop10ReportVO = orderService.getSalesTop10(reportDTO);
        return Result.success(salesTop10ReportVO);
    }

}
