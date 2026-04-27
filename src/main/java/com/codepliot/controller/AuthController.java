package com.codepliot.controller;

import com.codepliot.model.LoginRequest;
import com.codepliot.model.RegisterRequest;
import com.codepliot.service.AuthService;
import com.codepliot.model.LoginResponse;
import com.codepliot.model.Result;
import com.codepliot.model.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * AuthController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
/**
 * 创建 AuthController 实例。
 */
public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /**
     * 执行 register 相关逻辑。
     */
@PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("Register success", authService.register(request));
    }
    /**
     * 执行 login 相关逻辑。
     */
@PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("Login success", authService.login(request));
    }
}


