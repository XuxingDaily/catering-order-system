package com.catering.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 套餐菜品关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("套餐菜品关系")
public class SetmealDish implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("套餐菜品关系ID")
    private Long id;

    @ApiModelProperty("套餐ID")
    private Long setmealId;

    @ApiModelProperty("菜品ID")
    private Long dishId;

    @ApiModelProperty("菜品名称")
    private String name;

    @ApiModelProperty("菜品原价")
    private BigDecimal price;

    @ApiModelProperty("菜品份数")
    private Integer copies;

}
