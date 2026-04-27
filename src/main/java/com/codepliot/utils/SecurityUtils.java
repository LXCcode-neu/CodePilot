package com.codepliot.utils;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.LoginUser;
import com.codepliot.model.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * SecurityUtils 工具类，提供通用辅助方法。
 */
public final class SecurityUtils {
/**
 * 创建 SecurityUtils 实例。
 */
private SecurityUtils() {
    }
/**
 * 获取Login User相关逻辑。
 */
public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User is not authenticated");
        }
        return loginUser;
    }
/**
 * 获取Current User Id相关逻辑。
 */
public static Long getCurrentUserId() {
        return getLoginUser().getUserId();
    }
/**
 * 获取Current Username相关逻辑。
 */
public static String getCurrentUsername() {
        return getLoginUser().getUsername();
    }
}


