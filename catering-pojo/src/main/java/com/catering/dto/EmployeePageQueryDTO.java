package com.catering.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "员工分页查询传递的请求参数")
public class EmployeePageQueryDTO implements Serializable {

    @ApiModelProperty("员工姓名")
    private String name;

    @ApiModelProperty("当前页码")
    private int page;

    @ApiModelProperty("每页显示记录数")
    private int pageSize;

}
