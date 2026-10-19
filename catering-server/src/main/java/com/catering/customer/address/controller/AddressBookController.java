package com.catering.customer.address.controller;

import com.catering.shared.context.BaseContext;
import com.catering.entity.AddressBook;
import com.catering.shared.result.Result;
import com.catering.customer.address.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api(tags = "地址簿接口")
@RequiredArgsConstructor
@RequestMapping("/user/addressBook")
public class AddressBookController {

    private final AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result save(@RequestBody AddressBook addressBook) {
        log.info("新增地址 {}", addressBook);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookService.save(addressBook);
        return Result.success();
    }

    /**
     * 查询当前登录用户的所有地址信息
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询当前登录用户的所有地址信息")
    public Result<List<AddressBook>> list() {
        log.info("查询当前登录用户的所有地址信息");
        Long userId = BaseContext.getCurrentId();
        List<AddressBook> addressBook = addressBookService.list(userId);
        return Result.success(addressBook);
    }

    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookService.getDefaultByUserId(userId);
        return Result.success(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result delete(@RequestParam Long id) {
        log.info("根据id删除地址 {}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> get(@PathVariable Long id) {
        log.info("根据id查询地址 {}", id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result update(@RequestBody AddressBook addressBook) {
        log.info("根据id修改地址 {}", addressBook);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookService.update(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result updateDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址");
        Long userId = BaseContext.getCurrentId();
        Long AddressBookId = addressBook.getId();
        addressBookService.setDefault(userId, AddressBookId);
        return Result.success();
    }

}
