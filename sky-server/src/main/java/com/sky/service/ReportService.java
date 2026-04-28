package com.sky.service;

import com.sky.dto.ReportDTO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;

public interface ReportService {

    /**
     * 营业额统计
     * @param turnoverReportDTO
     * @return
     */
    TurnoverReportVO turnoverStatistics(ReportDTO turnoverReportDTO);


    /**
     * 用户统计
     * @param reportDTO
     * @return
     */
    UserReportVO userStatistics(ReportDTO reportDTO);

    /**
     * 订单统计
     * @param reportDTO
     * @return
     */
    OrderReportVO orderStatistics(ReportDTO reportDTO);

    /**
     * 查询销量排名top10接口
     * @param reportDTO
     * @return
     */
    SalesTop10ReportVO getSalesTop10(ReportDTO reportDTO);

    /**
     * 导出报表数据
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
