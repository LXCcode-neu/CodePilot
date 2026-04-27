package com.codepliot.model;

import com.codepliot.entity.User;
import java.time.LocalDateTime;
/**
 * UserVO 模型类，用于承载流程中的数据结构。
 */
public record UserVO(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
/**
 * 执行 from 相关逻辑。
 */
public static UserVO from(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}


