package com.codepliot.model;

import com.codepliot.entity.User;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
/**
 * LoginUser 模型类，用于承载流程中的数据结构。
 */
@Getter
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;

    private final String username;

    private final String password;

    private final String email;
/**
 * 创建 LoginUser 实例。
 */
public LoginUser(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
    }
    /**
     * 获取Authorities相关逻辑。
     */
@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    /**
     * 执行 isAccountNonExpired 相关逻辑。
     */
@Override
    public boolean isAccountNonExpired() {
        return true;
    }
    /**
     * 执行 isAccountNonLocked 相关逻辑。
     */
@Override
    public boolean isAccountNonLocked() {
        return true;
    }
    /**
     * 执行 isCredentialsNonExpired 相关逻辑。
     */
@Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    /**
     * 执行 isEnabled 相关逻辑。
     */
@Override
    public boolean isEnabled() {
        return true;
    }
}


