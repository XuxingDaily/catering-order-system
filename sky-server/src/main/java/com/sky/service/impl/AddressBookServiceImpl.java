package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 设置默认地址
     * @param userId
     * @param addressBookId
     */
    @Override
    public void setDefault(Long userId, Long addressBookId) {
        // 设置该用户的其他地址的 is_default 为 0
        addressBookMapper.updateIsDefaultByUserId(userId, 0);

        // 设置该用户对应id的地址 is_default 为 1
        addressBookMapper.updateById(AddressBook.builder().id(addressBookId).isDefault(1).build());
    }

    /**
     * 更新地址
     * @param addressBook
     */
    @Override
    public void update(AddressBook addressBook) {
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Override
    public AddressBook getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询默认地址
     * @param userId
     * @return
     */
    @Override
    public AddressBook getDefaultByUserId(Long userId) {
        return addressBookMapper.selectByUserIdAndDefault(userId);
    }

    /**
     * 查询当前登录用户的所有地址信息
     * @param userId
     * @return
     */
    @Override
    public List<AddressBook> list(Long userId) {
        return addressBookMapper.selectByUserId(userId);
    }

    /**
     * 新增地址
     * @param addressBook
     */
    @Override
    public void save(AddressBook addressBook) {
        addressBookMapper.insert(addressBook);
    }
}
