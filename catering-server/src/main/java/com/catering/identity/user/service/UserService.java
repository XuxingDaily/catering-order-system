package com.catering.identity.user.service;

import com.catering.dto.UserLoginDTO;
import com.catering.vo.UserLoginVO;

public interface UserService {

    /**
     * 微信用户授权登录
     * @param userLoginDTO
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

}
