package com.catering.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("新增/修改员工传入的数据模型")
public class EmployeeDTO implements Serializable {

    @ApiModelProperty("员工id")
    private Long id;

    @ApiModelProperty("员工用户名")
    private String username;

    @ApiModelProperty("员工姓名")
    private String name;

    @ApiModelProperty("员工手机号码")
    private String phone;

    @ApiModelProperty("员工性别")
    private String sex;

    @ApiModelProperty("员工身份证")
    private String idNumber;

}
