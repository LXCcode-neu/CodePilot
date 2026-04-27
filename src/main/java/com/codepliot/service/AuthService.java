package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.model.LoginRequest;
import com.codepliot.model.RegisterRequest;
import com.codepliot.utils.JwtUtils;
import com.codepliot.model.LoginUser;
import com.codepliot.utils.SecurityUtils;
import com.codepliot.model.LoginResponse;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.entity.User;
import com.codepliot.repository.UserMapper;
import com.codepliot.model.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
/**
 * AuthService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
/**
 * 创建 AuthService 实例。
 */
public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }
/**
 * 执行 register 相关逻辑。
 */
public UserVO register(RegisterRequest request) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username())
                .last("limit 1"));
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        userMapper.insert(user);
        return UserVO.from(user);
    }
/**
 * 执行 login 相关逻辑。
 */
public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        LoginUser loginUser = new LoginUser(user);
        String token = jwtUtils.generateToken(loginUser);
        return new LoginResponse(token, "Bearer", UserVO.from(user));
    }
/**
 * 执行 currentUser 相关逻辑。
 */
public UserVO currentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }
}



