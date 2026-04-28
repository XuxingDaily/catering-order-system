package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.ReportDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信用户授权登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        // 获取 openId
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("js_code", userLoginDTO.getCode());
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, paramMap);

        String openId = JSON.parseObject(json).getString("openid");
        if (openId == null || openId.isEmpty()) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 访问数据库，查询是否有当前用户
        User user = userMapper.selectByOpenId(openId);
        if (user == null) {
            // 用户不存在时自动注册，插入到数据库
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            // 注意：需要回显id
            userMapper.insert(user);
        }

        // 为当前用户生成 jwt 令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        // 构建 VO 返回
        return UserLoginVO.builder().id(user.getId()).openid(openId).token(token).build();
    }

    /**
     * 用户统计
     * @param reportDTO
     * @return
     */
    @Override
    public UserReportVO userStatistics(ReportDTO reportDTO) {
        LocalDate beginDate = reportDTO.getBegin();
        LocalDate endDate = reportDTO.getEnd();

        // 生成完整的日期列表 (用于填充和VO返回)
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate tempDate = beginDate;
        while (!tempDate.isAfter(endDate)) {
            dateList.add(tempDate);
            tempDate = tempDate.plusDays(1);
        }

        // 查询数据库 - 新增用户列表
        LocalDateTime begin = beginDate.atStartOfDay();
        LocalDateTime end = begin.plusDays(1);
        List<Long> newUserList = getUserList(dateList, begin, end);

        // 查询数据库 - 总用户
        List<Long> totalUserList = getUserList(dateList, null, null);

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ','))
                .newUserList(StringUtils.join(newUserList, ','))
                .totalUserList(StringUtils.join(totalUserList, ','))
                .build();
    }

    private List<Long> getUserList(List<LocalDate> dateList, LocalDateTime begin, LocalDateTime end) {
        List<Map<String, Object>> totalResultMap = userMapper.getUserStatistics(begin, end);
        Map<String, Long> userMap = totalResultMap.stream().collect(Collectors.toMap(
                m -> m.get("createTime").toString(),
                m -> (Long) m.get("total")
        ));
        return dateList.stream().map(date -> {
            Long total = userMap.get(date.toString());
            return total == null ? 0 : total;
        }).collect(Collectors.toList());
    }

}
