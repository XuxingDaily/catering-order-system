package com.sky.service;

import com.sky.dto.ReportDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.vo.UserLoginVO;
import com.sky.vo.UserReportVO;

public interface UserService {

    /**
     * 微信用户授权登录
     * @param userLoginDTO
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

}
