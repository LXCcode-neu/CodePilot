package com.codepliot.utils;

import com.codepliot.config.JwtProperties;
import com.codepliot.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
/**
 * JwtUtils 工具类，提供通用辅助方法。
 */
@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;
/**
 * 创建 JwtUtils 实例。
 */
public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }
/**
 * 执行 generateToken 相关逻辑。
 */
public String generateToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claims(Map.of(
                        "userId", loginUser.getUserId(),
                        "email", loginUser.getEmail()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSecretKey())
                .compact();
    }
/**
 * 获取Username From Token相关逻辑。
 */
public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }
/**
 * 获取User Id From Token相关逻辑。
 */
public Long getUserIdFromToken(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (userId instanceof Long longValue) {
            return longValue;
        }
        return Long.valueOf(String.valueOf(userId));
    }
/**
 * 校验Token相关逻辑。
 */
public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }
/**
 * 解析Claims相关逻辑。
 */
private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
/**
 * 获取Secret Key相关逻辑。
 */
private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}


