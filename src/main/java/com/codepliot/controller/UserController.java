package com.codepliot.controller;

import com.codepliot.service.AuthService;
import com.codepliot.model.Result;
import com.codepliot.model.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * UserController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
/**
 * 创建 UserController 实例。
 */
public UserController(AuthService authService) {
        this.authService = authService;
    }
    /**
     * 获取Current User相关逻辑。
     */
@GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(authService.currentUser());
    }
}


