package com.codepliot.user.controller;

import com.codepliot.auth.service.AuthService;
import com.codepliot.common.result.Result;
import com.codepliot.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(authService.currentUser());
    }
}
