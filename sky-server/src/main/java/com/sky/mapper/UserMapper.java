package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * 根据 openId 查询用户
     * @param openId
     * @return
     */
    @Select("SELECT * FROM user WHERE openid = #{openId}")
    User selectByOpenId(String openId);

    /**
     * 插入新用户
     * @param user
     */
    void insert(User user);

    /**
     * 根据id搜索用户
     * @param userId
     * @return
     */
    @Select("SELECT * FROM user WHERE id = #{userId}")
    User selectById(Long userId);
}
