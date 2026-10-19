package com.catering.identity.user.mapper;

import com.catering.entity.User;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @MapKey("createTime")
    List<Map<String, Object>> getUserStatistics(LocalDateTime begin, LocalDateTime end);

    /**
     * 通过时间区间查询用户数
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select("SELECT COUNT(1) FROM user WHERE create_time > #{beginTime} AND create_time < #{endTime}")
    Integer selectUserByTime(LocalDateTime beginTime, LocalDateTime endTime);
}
