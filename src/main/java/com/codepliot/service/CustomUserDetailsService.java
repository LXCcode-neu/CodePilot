package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.model.LoginUser;
import com.codepliot.entity.User;
import com.codepliot.repository.UserMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
/**
 * CustomUserDetailsService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
/**
 * 创建 CustomUserDetailsService 实例。
 */
public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    /**
     * 加载User By Username相关逻辑。
     */
@Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new LoginUser(user);
    }
}


