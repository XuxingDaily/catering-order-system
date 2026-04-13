package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修改密码传入的请求体参数")
public class EmployeeEditPasswordDTO {

    @ApiModelProperty("员工id")
    private Integer empId;

    @ApiModelProperty("新密码")
    private String newPassword;

    @ApiModelProperty("旧密码")
    private String oldPassword;

}
