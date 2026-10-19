package com.sky.customer.address.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    /**
     * 插入新地址
     * @param addressBook
     */
    @Insert("INSERT INTO address_book" +
            "(user_id, consignee, sex, phone, province_code, province_name, " +
            "city_code, city_name, district_code, district_name, detail, label, is_default) " +
            "VALUES " +
            "(#{userId}, #{consignee}, #{sex}, #{phone}, #{provinceCode}, #{provinceName}, " +
            "#{cityCode}, #{cityName}, #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    void insert(AddressBook addressBook);

    /**
     * 根据用户id查询地址列表
     * @param userId
     * @return
     */
    @Select("SELECT * FROM address_book WHERE user_id = #{userId}")
    List<AddressBook> selectByUserId(Long userId);

    /**
     * 查询默认地址
     * @param userId
     * @return
     */
    @Select("SELECT * FROM address_book WHERE user_id = #{userId} AND is_default = 1")
    AddressBook selectByUserIdAndDefault(Long userId);

    /**
     * 根据id删除地址
     * @param id
     */
    @Delete("DELETE FROM address_book WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Select("SELECT * FROM address_book WHERE id = #{id}")
    AddressBook selectById(Long id);

    /**
     * 更新地址
     * @param addressBook
     */
    void updateById(AddressBook addressBook);

    /**
     * 根据用户id设置默认地址
     * @param userId
     * @param isDefault
     */
    @Update("UPDATE address_book SET is_default = #{isDefault} WHERE user_id = #{userId}")
    void updateIsDefaultByUserId(Long userId, Integer isDefault);
}
