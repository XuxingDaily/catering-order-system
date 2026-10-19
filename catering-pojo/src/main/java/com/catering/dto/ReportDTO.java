package com.catering.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("营业额统计输入的条件日期")
public class ReportDTO {
    @ApiModelProperty("开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate begin;

    @ApiModelProperty("结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate end;
}
