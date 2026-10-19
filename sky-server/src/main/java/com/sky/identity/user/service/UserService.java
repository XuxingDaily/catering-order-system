package com.sky.identity.user.service;

import com.sky.dto.UserLoginDTO;
import com.sky.vo.UserLoginVO;

public interface UserService {

    /**
     * 微信用户授权登录
     * @param userLoginDTO
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

}
