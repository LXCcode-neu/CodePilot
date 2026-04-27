package com.codepliot.model;

import com.codepliot.model.UserVO;
/**
 * LoginResponse 模型类，用于承载流程中的数据结构。
 */
public record LoginResponse(
        String token,
        String tokenType,
        UserVO user
) {
}


