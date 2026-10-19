package com.sky.catalog.shop.controller.admin;

import com.sky.shared.constant.RedisKeyConstant;
import com.sky.shared.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺的营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺的营业状态")
    public Result updateStatus(@PathVariable Integer status) {
        log.info("设置店铺的营业状态为 {}", status == 1 ? "营业中" : "已打烊");
        redisTemplate.opsForValue().set(RedisKeyConstant.SHOP_STATUS_KEY, status);
        return Result.success();
    }

    /**
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS_KEY);
        log.info("获取到店铺的营业状态为 {}", 1 == status ? "营业中" : "已打烊");
        return Result.success(status);
    }

}
