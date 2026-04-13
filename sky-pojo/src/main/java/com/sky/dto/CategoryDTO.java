package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("新增 / 修改分类的输入请求体参数")
public class CategoryDTO implements Serializable {

    @ApiModelProperty("分类名称")
    private Long id;

    @ApiModelProperty("分类类型：类型 1 菜品分类 2 套餐分类")
    private Integer type;

    @ApiModelProperty("分类名称")
    private String name;

    @ApiModelProperty("排序")
    private Integer sort;

}
