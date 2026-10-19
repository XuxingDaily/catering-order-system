package com.catering.operation.report.service;

import com.catering.dto.ReportDTO;
import com.catering.vo.OrderReportVO;
import com.catering.vo.SalesTop10ReportVO;
import com.catering.vo.TurnoverReportVO;
import com.catering.vo.UserReportVO;

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
