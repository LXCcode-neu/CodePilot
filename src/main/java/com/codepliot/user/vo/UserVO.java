package com.codepliot.user.vo;

import com.codepliot.user.entity.User;
import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

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
