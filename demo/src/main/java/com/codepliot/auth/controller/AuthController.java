package com.codepliot.auth.controller;

import com.codepliot.auth.dto.LoginRequest;
import com.codepliot.auth.dto.RegisterRequest;
import com.codepliot.auth.service.AuthService;
import com.codepliot.auth.vo.LoginResponse;
import com.codepliot.common.result.Result;
import com.codepliot.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("Register success", authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("Login success", authService.login(request));
    }
}
