package com.codepliot.auth.vo;

import com.codepliot.user.vo.UserVO;

public record LoginResponse(
        String token,
        String tokenType,
        UserVO user
) {
}
