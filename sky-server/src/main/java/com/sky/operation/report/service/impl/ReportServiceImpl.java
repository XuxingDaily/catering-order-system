package com.sky.operation.report.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.ReportDTO;
import com.sky.entity.Orders;
import com.sky.trade.order.mapper.OrderMapper;
import com.sky.identity.user.mapper.UserMapper;
import com.sky.operation.report.service.ReportService;
import com.sky.operation.workspace.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate tempDate = begin;
        while (!tempDate.isAfter(end)) {
            dateList.add(tempDate);
            tempDate = tempDate.plusDays(1);
        }
        return dateList;
    }

    private List<Long> getUserList(List<LocalDate> dateList, LocalDateTime begin, LocalDateTime end) {
        List<Map<String, Object>> totalResultMap = userMapper.getUserStatistics(begin, end);
        Map<String, Long> userMap = totalResultMap.stream().collect(Collectors.toMap(
                m -> m.get("createTime").toString(),
                m -> (Long) m.get("total")
        ));
        return dateList.stream().map(date -> {
            Long total = userMap.get(date.toString());
            return total == null ? 0 : total;
        }).collect(Collectors.toList());
    }

    private List<Long> getOrderStatistics(List<LocalDate> dateList, LocalDateTime begin, LocalDateTime end, Integer status) {
        List<Map<String, Object>> resultMap = orderMapper.getOrderStatistics(begin, end, status);
        Map<String, Object> orderMap = resultMap.stream().collect(Collectors.toMap(
                m -> m.get("orderDate").toString(),
                m -> (Long) m.get("total")
        ));
        return dateList.stream().map(date -> {
            Long orderCount = (Long) orderMap.get(date.toString());
            return orderCount == null ? 0 : orderCount;
        }).collect(Collectors.toList());
    }

    /**
     * 营业额统计
     * @param reportDTO
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(ReportDTO reportDTO) {
        LocalDate beginDate = reportDTO.getBegin();
        LocalDate endDate = reportDTO.getEnd();
        List<LocalDate> dateList = getDateList(beginDate, endDate);

        // 查询数据库
        LocalDateTime begin = beginDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        Integer status = Orders.COMPLETED;
        List<Map<String, Object>> resultMap = orderMapper.getTurnoverStatistics(begin, end, status);
        Map<String, BigDecimal> turnoverMap = resultMap.stream().collect(Collectors.toMap(
                m -> m.get("orderDate").toString(),
                m -> (BigDecimal) m.get("turnover")
        ));
        List<BigDecimal> turnoverList = dateList.stream().map(date -> {
            BigDecimal amount = turnoverMap.get(date.toString());
            return amount == null ? BigDecimal.ZERO : amount;
        }).collect(Collectors.toList());

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param reportDTO
     * @return
     */
    @Override
    public UserReportVO userStatistics(ReportDTO reportDTO) {
        LocalDate beginDate = reportDTO.getBegin();
        LocalDate endDate = reportDTO.getEnd();

        // 生成完整的日期列表 (用于填充和VO返回)
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate tempDate = beginDate;
        while (!tempDate.isAfter(endDate)) {
            dateList.add(tempDate);
            tempDate = tempDate.plusDays(1);
        }

        // 查询数据库 - 新增用户列表
        LocalDateTime begin = beginDate.atStartOfDay();
        LocalDateTime end = begin.plusDays(1);
        List<Long> newUserList = getUserList(dateList, begin, end);

        // 查询数据库 - 总用户
        List<Long> totalUserList = getUserList(dateList, null, null);

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ','))
                .newUserList(StringUtils.join(newUserList, ','))
                .totalUserList(StringUtils.join(totalUserList, ','))
                .build();
    }

    /**
     * 订单统计
     * @param reportDTO
     * @return
     */
    @Override
    public OrderReportVO orderStatistics(ReportDTO reportDTO) {
        LocalDate beginDate = reportDTO.getBegin();
        LocalDate endDate = reportDTO.getEnd();
        List<LocalDate> dateList = getDateList(beginDate, endDate);

        // 查询数据库 - 有效订单数列表
        LocalDateTime begin = beginDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        Integer status = Orders.COMPLETED;
        List<Long> validOrderCountList = getOrderStatistics(dateList, begin, end, status);
        long validOrderCount = validOrderCountList.stream().reduce(Long::sum).get();

        // 查询数据库 - 订单数列表
        List<Long> orderCountList = getOrderStatistics(dateList, begin, end, null);
        long totalOrderCount = orderCountList.stream().reduce(Long::sum).get();

        // 订单完成率
        Double orderCompletionRate = totalOrderCount != 0 ? 1.0 * validOrderCount / totalOrderCount : 1.0;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ','))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ','))
                .totalOrderCount((int) totalOrderCount)
                .validOrderCount((int) validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList, ','))
                .build();
    }

    /**
     * 查询销量排名top10接口
     * @param reportDTO
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(ReportDTO reportDTO) {
        LocalDateTime beginTime = reportDTO.getBegin().atStartOfDay();
        LocalDateTime endTime = reportDTO.getEnd().plusDays(1).atStartOfDay();

        // 查询数据库
        Integer status = Orders.COMPLETED;
        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.getSalesStatistics(beginTime, endTime, status);
        List<String> nameList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ','))
                .numberList(StringUtils.join(numberList, ','))
                .build();
    }

    /**
     * 导出报表数据
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 查询近三十天的营业数据总览
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(beginDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        // 通过 POI 写入报表模板中
        try (
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
                XSSFWorkbook excel = new XSSFWorkbook(Objects.requireNonNull(in));
                ServletOutputStream out = response.getOutputStream();
        ) {
            XSSFSheet sheet = excel.getSheet("Sheet1");
            XSSFRow row;

            // 时间行
            sheet.getRow(1).getCell(1).setCellValue("时间：" + beginDate + " 至 " + endDate);

            // 总览第一行
            row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            // 总览第二行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            // 明细
            int i = 7;
            for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1), i++) {
                LocalDateTime beginTime = date.atStartOfDay();
                LocalDateTime endTime = date.plusDays(1).atStartOfDay();
                businessData = workspaceService.getBusinessData(beginTime, endTime);

                row = sheet.getRow(i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 通过输出流将生成的 excel 对象下载到客户端浏览器
            excel.write(out);
        } catch (Exception e) {
            log.error("报表输出出现错误：" + e.getMessage(), e);
        }

    }
}
