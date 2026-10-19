package com.sky.customer.address.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    /**
     * 新增地址
     * @param addressBook
     */
    void save(AddressBook addressBook);

    /**
     * 查询当前登录用户的所有地址信息
     * @param userId
     * @return
     */
    List<AddressBook> list(Long userId);

    /**
     * 查询默认地址
     * @param userId
     * @return
     */
    AddressBook getDefaultByUserId(Long userId);

    /**
     * 根据id删除地址
     * @param id
     */
    void deleteById(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    AddressBook getById(Long id);

    /**
     * 根据 addressBook.id 更新地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param userId
     * @param addressBookId
     */
    void setDefault(Long userId, Long addressBookId);

}
